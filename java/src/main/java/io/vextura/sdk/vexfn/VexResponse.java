package io.vextura.sdk.vexfn;

import java.util.Map;

/**
 * Wire format written to stdout (one JSON line per response).
 * Aligned with smithy/traits/vexfn.smithy#VexResponse.
 */
public record VexResponse(
        String eventId,
        boolean success,
        Map<String, Object> outputData,
        String errorCode,
        String errorMessage,
        Long durationMs
) {
    /** Convenience factory for a successful response. */
    public static VexResponse ok(String eventId, Map<String, Object> outputData, long durationMs) {
        return new VexResponse(eventId, true, outputData, null, null, durationMs);
    }

    /** Convenience factory for an error response. */
    public static VexResponse error(String eventId, String errorCode, String errorMessage) {
        return new VexResponse(eventId, false, null, errorCode, errorMessage, null);
    }
}
