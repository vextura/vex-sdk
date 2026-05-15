package io.vextura.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StepInfo(
    @JsonProperty("name")           String name,
    @JsonProperty("childStepCount") int childStepCount,
    @JsonProperty("isParallel")     boolean isParallel
) {}
