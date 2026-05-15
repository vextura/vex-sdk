package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PollExecutionResultResponse(
    @JsonProperty("runId")                  String runId,
    @JsonProperty("status")                String status,
    @JsonProperty("result")               ExecutionResult result,
    @JsonProperty("pollAfterMs")           Integer pollAfterMs,
    @JsonProperty("estimatedCompletionMs") Integer estimatedCompletionMs,
    @JsonProperty("progress")             Float progress
) {}
