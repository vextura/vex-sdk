package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionInfo(
    @JsonProperty("runId")                 String runId,
    @JsonProperty("workflowId")            String workflowId,
    @JsonProperty("status")               String status,
    @JsonProperty("currentStepIndex")      int currentStepIndex,
    @JsonProperty("currentChildStepIndex") int currentChildStepIndex,
    @JsonProperty("startTime")             Instant startTime,
    @JsonProperty("endTime")              Instant endTime,
    @JsonProperty("errorMessage")          String errorMessage,
    @JsonProperty("isTerminal")           boolean isTerminal,
    @JsonProperty("isRunning")            boolean isRunning,
    @JsonProperty("isPending")            boolean isPending,
    @JsonProperty("createdAt")             Instant createdAt,
    @JsonProperty("updatedAt")             Instant updatedAt
) {}
