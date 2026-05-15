package io.vextura;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vextura.auth.VexAuth;
import io.vextura.model.*;
import io.vextura.polling.ExecutionPoller;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Vextura Workflow Engine — Java 21 SDK client.
 *
 * <p>Source of truth: {@code api/workflow-api.smithy}
 *
 * <p>Usage:
 * <pre>{@code
 * var client = VexClient.builder()
 *     .endpoint("http://engine.vextura.io:8080")
 *     .licenseKey("VX-PRO-A4B2-9F3C-D7E1-2K8M")
 *     .build();
 *
 * // Fire and wait (most common pattern)
 * var result = client.executeAndWait("transaction-validation",
 *     Map.of("amount", 5000, "currency", "KZT"));
 *
 * // Async execute + manual polling
 * var run = client.workflows().asyncExecute("transaction-validation", Map.of("amount", 5000));
 * var status = client.executions().getStatus(run.runId());
 * }</pre>
 */
public final class VexClient implements AutoCloseable {

    private static final String SDK_VERSION = "1.2.0";
    private static final String SDK_HEADER   = "X-Vextura-SDK";
    private static final String SDK_HEADER_VALUE = "java21/" + SDK_VERSION;

    private final String endpoint;
    private final VexAuth auth;
    private final CloseableHttpClient http;
    private final ObjectMapper mapper;
    private final int maxRetries;
    private final long retryBaseDelayMs;

    private final WorkflowsClient workflowsClient;
    private final ExecutionsClient executionsClient;
    private final ExecutionPoller poller;

    private VexClient(Builder builder) {
        this.endpoint         = builder.endpoint.replaceAll("/+$", "");
        this.auth             = builder.auth;
        this.maxRetries       = builder.maxRetries;
        this.retryBaseDelayMs = builder.retryBaseDelayMs;

        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.http = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(builder.timeoutMs, TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(builder.timeoutMs, TimeUnit.MILLISECONDS))
                .build())
            .build();

        this.workflowsClient  = new WorkflowsClient();
        this.executionsClient = new ExecutionsClient();
        this.poller           = new ExecutionPoller(executionsClient, builder.timeoutMs);
    }

    // ─── Factory ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    // ─── Sub-clients ──────────────────────────────────────────

    /** Workflow management operations. */
    public WorkflowsClient workflows() {
        return workflowsClient;
    }

    /** Execution management operations. */
    public ExecutionsClient executions() {
        return executionsClient;
    }

    // ─── Top-level convenience ────────────────────────────────

    /**
     * Execute a workflow and block until it completes or the timeout elapses.
     *
     * @param workflowId  registered workflow ID or name
     * @param inputData   key-value input payload
     * @return            final execution result
     * @throws VexExecutionTimeoutException if timeout elapses before completion
     * @throws VexException                 on engine or network error
     */
    public ExecutionResult executeAndWait(String workflowId, Map<String, Object> inputData) {
        var run = workflowsClient.asyncExecute(workflowId, inputData);
        return poller.waitForCompletion(run.runId());
    }

    /**
     * Execute a workflow and block with a custom timeout.
     *
     * @param timeoutMs maximum wait time in milliseconds
     */
    public ExecutionResult executeAndWait(
            String workflowId,
            Map<String, Object> inputData,
            long timeoutMs) {
        var run = workflowsClient.asyncExecute(workflowId, inputData);
        return poller.waitForCompletion(run.runId(), timeoutMs);
    }

    // ─── Health ───────────────────────────────────────────────

    public boolean isHealthy() {
        try {
            var req = new HttpGet(endpoint + "/health");
            applyHeaders(req);
            return http.execute(req, response -> {
                var body = mapper.readValue(response.getEntity().getContent(), Map.class);
                return "healthy".equals(body.get("status"));
            });
        } catch (IOException e) {
            return false;
        }
    }

    // ─── Inner: WorkflowsClient ───────────────────────────────

    public final class WorkflowsClient {

        public ListWorkflowsResponse list() {
            return get("/api/v1/workflows", ListWorkflowsResponse.class);
        }

        public WorkflowInfo get(String workflowId) {
            return get("/api/v1/workflows/" + workflowId, WorkflowInfo.class);
        }

        /**
         * Trigger asynchronous workflow execution.
         * Returns immediately with a {@code runId} — use
         * {@code executions().getStatus(runId)} to poll.
         */
        public AsyncExecuteResponse asyncExecute(
                String workflowId,
                Map<String, Object> inputData) {
            return post(
                "/api/v1/workflows/" + workflowId + "/async-execute",
                inputData,
                AsyncExecuteResponse.class);
        }

        private <T> T get(String path, Class<T> type) {
            return VexClient.this.get(path, type);
        }

        private <T> T post(String path, Object body, Class<T> type) {
            return VexClient.this.post(path, body, type);
        }
    }

    // ─── Inner: ExecutionsClient ──────────────────────────────

    public final class ExecutionsClient {

        public ExecutionStatus getStatus(String runId) {
            return get("/api/v1/executions/" + runId, ExecutionStatus.class);
        }

        public ExecutionResult getResult(String runId) {
            return get("/api/v1/executions/" + runId + "/result", PollExecutionResultResponse.class)
                .result();
        }

        public ExecutionMetrics getMetrics(String runId) {
            return get("/api/v1/executions/" + runId + "/metrics", ExecutionMetrics.class);
        }

        public ListExecutionsResponse list(
                String workflowId, String status, int limit, int offset) {
            var query = buildQuery(workflowId, status, limit, offset);
            return get("/api/v1/executions" + query, ListExecutionsResponse.class);
        }

        public void cancel(String runId) {
            VexClient.this.post("/api/v1/executions/" + runId + "/cancel", null, Void.class);
        }

        public void pause(String runId) {
            VexClient.this.post("/api/v1/executions/" + runId + "/pause", null, Void.class);
        }

        public void resume(String runId) {
            VexClient.this.post("/api/v1/executions/" + runId + "/resume", null, Void.class);
        }

        public void retry(String runId) {
            VexClient.this.post("/api/v1/executions/" + runId + "/retry", null, Void.class);
        }

        private <T> T get(String path, Class<T> type) {
            return VexClient.this.get(path, type);
        }

        private String buildQuery(String workflowId, String status, int limit, int offset) {
            var sb = new StringBuilder("?limit=").append(limit).append("&offset=").append(offset);
            if (workflowId != null) sb.append("&workflow_id=").append(workflowId);
            if (status != null)     sb.append("&status=").append(status);
            return sb.toString();
        }
    }

    // ─── HTTP primitives ──────────────────────────────────────

    private <T> T get(String path, Class<T> type) {
        return withRetry(() -> {
            var req = new HttpGet(endpoint + path);
            applyHeaders(req);
            return http.execute(req, response -> {
                checkStatus(response.getCode(), path);
                if (type == Void.class) return null;
                return mapper.readValue(response.getEntity().getContent(), type);
            });
        });
    }

    private <T> T post(String path, Object body, Class<T> type) {
        return withRetry(() -> {
            var req = new HttpPost(endpoint + path);
            applyHeaders(req);
            if (body != null) {
                req.setEntity(new StringEntity(mapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            }
            return http.execute(req, response -> {
                checkStatus(response.getCode(), path);
                if (type == Void.class) return null;
                return mapper.readValue(response.getEntity().getContent(), type);
            });
        });
    }

    private void applyHeaders(org.apache.hc.core5.http.HttpRequest req) {
        auth.applyTo(req);
        req.setHeader(SDK_HEADER, SDK_HEADER_VALUE);
    }

    private void checkStatus(int code, String path) {
        if (code == 401 || code == 403) throw new VexAuthException();
        if (code == 404)                throw new VexNotFoundException("Resource not found: " + path);
        if (code >= 400)                throw new VexException("HTTP " + code + " for " + path, code);
    }

    private <T> T withRetry(IOSupplier<T> fn) {
        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return fn.get();
            } catch (VexException e) {
                if (e.statusCode() < 500) throw e; // 4xx — don't retry
                last = e;
            } catch (Exception e) {
                last = e;
            }
            if (attempt < maxRetries) {
                try { Thread.sleep(retryBaseDelayMs * (1L << attempt)); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw new VexException("Max retries exceeded", 503, last);
    }

    @Override
    public void close() throws IOException {
        http.close();
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws Exception;
    }

    // ─── Builder ──────────────────────────────────────────────

    public static final class Builder {
        private String  endpoint;
        private VexAuth auth         = VexAuth.none();
        private long    timeoutMs    = 30_000;
        private int     maxRetries   = 3;
        private long    retryBaseDelayMs = 500;

        /**
         * Base URL of the Vextura engine.
         * Example: {@code http://engine.vextura.io:8080}
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
            return this;
        }

        /**
         * Vextura portal-issued license key.
         * Sent as {@code X-Vextura-License} header.
         * This is the recommended auth method for all production deployments.
         */
        public Builder licenseKey(String licenseKey) {
            this.auth = VexAuth.licenseKey(licenseKey);
            return this;
        }

        /**
         * Bearer token for banks with existing JWT/OIDC infrastructure.
         * Sent as {@code Authorization: Bearer <token>}.
         */
        public Builder bearerToken(String token) {
            this.auth = VexAuth.bearer(token);
            return this;
        }

        /** HTTP request timeout in milliseconds. Default: 30000 */
        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /** Max retries on 5xx / network failures. Default: 3 */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public VexClient build() {
            Objects.requireNonNull(endpoint, "endpoint is required");
            return new VexClient(this);
        }
    }
}
