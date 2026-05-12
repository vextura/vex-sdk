package io.vextura.sdk.vexfn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Wire format received from vex-executor on stdin (one JSON line per event).
 * Aligned with smithy/traits/vexfn.smithy#VexEvent.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VexEvent(
        String eventId,
        String workflowRunId,
        String stepId,
        Map<String, Object> inputData,
        EventMetadata metadata
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventMetadata(
            String traceId,
            String spanId,
            String tenantId,
            Integer priority
    ) {}
}
