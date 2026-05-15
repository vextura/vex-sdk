package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionMetrics(
    @JsonProperty("runId")               String runId,
    @JsonProperty("workflowId")          String workflowId,
    @JsonProperty("totalSteps")          int totalSteps,
    @JsonProperty("completedSteps")      int completedSteps,
    @JsonProperty("failedSteps")         int failedSteps,
    @JsonProperty("totalChildSteps")     int totalChildSteps,
    @JsonProperty("completedChildSteps") int completedChildSteps,
    @JsonProperty("failedChildSteps")    int failedChildSteps,
    @JsonProperty("totalDurationMillis") long totalDurationMillis,
    @JsonProperty("averageStepDuration") float averageStepDuration,
    @JsonProperty("successRate")        float successRate
) {}
