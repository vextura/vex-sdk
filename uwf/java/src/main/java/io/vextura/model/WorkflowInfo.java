package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowInfo(
    @JsonProperty("workflowId")   String workflowId,
    @JsonProperty("name")         String name,
    @JsonProperty("description")  String description,
    @JsonProperty("stepCount")    int stepCount,
    @JsonProperty("steps")        List<StepInfo> steps
) {}
