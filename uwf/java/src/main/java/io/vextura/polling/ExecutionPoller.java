package io.vextura.polling;

import io.vextura.VexClient.ExecutionsClient;
import io.vextura.VexExecutionTimeoutException;
import io.vextura.model.ExecutionResult;
import io.vextura.model.ExecutionStatus;

/**
 * Polls execution status until terminal state, with exponential backoff.
 */
public final class ExecutionPoller {

    private static final long DEFAULT_TIMEOUT_MS    = 120_000;
    private static final long INITIAL_POLL_MS       = 1_000;
    private static final long MAX_POLL_MS           = 5_000;

    private final ExecutionsClient executions;
    private final long             defaultTimeoutMs;

    public ExecutionPoller(ExecutionsClient executions, long defaultTimeoutMs) {
        this.executions       = executions;
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public ExecutionResult waitForCompletion(String runId) {
        return waitForCompletion(runId, defaultTimeoutMs);
    }

    public ExecutionResult waitForCompletion(String runId, long timeoutMs) {
        long deadline     = System.currentTimeMillis() + timeoutMs;
        long pollInterval = INITIAL_POLL_MS;

        while (System.currentTimeMillis() < deadline) {
            ExecutionStatus status = executions.getStatus(runId);

            if (status.isTerminal()) {
                if ("completed".equals(status.status())) {
                    return executions.getResult(runId);
                }
                throw new io.vextura.VexException(
                    "Execution " + runId + " " + status.status() +
                    (status.errorMessage() != null ? ": " + status.errorMessage() : ""),
                    500
                );
            }

            try {
                Thread.sleep(Math.min(pollInterval, MAX_POLL_MS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new io.vextura.VexException("Polling interrupted", 0);
            }

            pollInterval = Math.min((long) (pollInterval * 1.5), MAX_POLL_MS);
        }

        throw new VexExecutionTimeoutException(runId, timeoutMs);
    }
}
