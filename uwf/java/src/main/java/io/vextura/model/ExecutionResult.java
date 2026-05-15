package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionResult(
    @JsonProperty("runId")               String runId,
    @JsonProperty("workflowId")          String workflowId,
    @JsonProperty("status")             String status,
    @JsonProperty("result")             Map<String, Object> result,
    @JsonProperty("completedAt")         Instant completedAt,
    @JsonProperty("executionTimeMillis") Long executionTimeMillis,
    @JsonProperty("stepCount")           Integer stepCount,
    @JsonProperty("errorDetails")        String errorDetails
) {}
