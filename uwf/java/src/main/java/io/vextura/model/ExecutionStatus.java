package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionStatus(
    @JsonProperty("runId")                 String runId,
    @JsonProperty("workflowId")            String workflowId,
    @JsonProperty("status")               String status,
    @JsonProperty("currentStep")           String currentStep,
    @JsonProperty("currentStepIndex")      int currentStepIndex,
    @JsonProperty("currentChildStepIndex") int currentChildStepIndex,
    @JsonProperty("progress")             float progress,
    @JsonProperty("startTime")             Instant startTime,
    @JsonProperty("endTime")              Instant endTime,
    @JsonProperty("errorMessage")          String errorMessage,
    @JsonProperty("lastAttemptedStep")     String lastAttemptedStep,
    @JsonProperty("isTerminal")           boolean isTerminal,
    @JsonProperty("metadata")             Map<String, Object> metadata
) {}
