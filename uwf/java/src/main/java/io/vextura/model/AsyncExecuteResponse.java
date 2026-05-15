package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsyncExecuteResponse(
    @JsonProperty("runId")                  String runId,
    @JsonProperty("status")                String status,
    @JsonProperty("message")               String message,
    @JsonProperty("statusUrl")             String statusUrl,
    @JsonProperty("resultUrl")             String resultUrl,
    @JsonProperty("pollAfterMs")           int pollAfterMs,
    @JsonProperty("estimatedCompletionMs") int estimatedCompletionMs,
    @JsonProperty("expiresAt")             Instant expiresAt
) {}
