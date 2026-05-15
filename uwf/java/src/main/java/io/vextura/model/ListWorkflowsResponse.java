package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListWorkflowsResponse(
    @JsonProperty("workflows") List<WorkflowInfo> workflows,
    @JsonProperty("count")     int count
) {}
