package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListExecutionsResponse(
    @JsonProperty("executions") List<ExecutionInfo> executions,
    @JsonProperty("count")      int count,
    @JsonProperty("limit")      int limit,
    @JsonProperty("offset")     int offset
) {}
