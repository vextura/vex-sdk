package io.vextura.sdk.vexfn;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.Instant;

/**
 * Entry point for a VexEdge function container.
 *
 * <pre>{@code
 * public class Main {
 *     public static void main(String[] args) {
 *         VexFn.run((event, ctx) -> {
 *             String txId = (String) event.inputData().get("transactionId");
 *             return VexResponse.ok(event.eventId(), Map.of("approved", true), 0L);
 *         });
 *     }
 * }
 * }</pre>
 */
public final class VexFn {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VexFn() {}

    /**
     * Start the stdin/stdout processing loop.
     * Blocks until stdin is closed (container shutdown).
     */
    public static void run(VexFnHandler handler) {
        String primUrl  = System.getenv("VEX_PRIMITIVES_URL");
        PrimitivesClient primitives = primUrl != null ? new PrimitivesClient(primUrl) : null;

        PrintStream out = System.out;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String line;
        try {
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;

                VexEvent event;
                try {
                    event = MAPPER.readValue(line, VexEvent.class);
                } catch (Exception e) {
                    writeResponse(out, VexResponse.error("", "PARSE_ERROR", e.getMessage()));
                    continue;
                }

                Context ctx = new Context(
                        primitives,
                        event.metadata() != null ? event.metadata().tenantId() : null,
                        event.metadata() != null ? event.metadata().traceId()  : null
                );

                long start = Instant.now().toEpochMilli();
                VexResponse resp;
                try {
                    resp = handler.handle(event, ctx);
                    if (resp == null) {
                        resp = VexResponse.ok(event.eventId(), null, 0L);
                    }
                    long dur = Instant.now().toEpochMilli() - start;
                    resp = new VexResponse(
                            event.eventId(), resp.success(), resp.outputData(),
                            resp.errorCode(), resp.errorMessage(), dur);
                } catch (Exception e) {
                    resp = VexResponse.error(event.eventId(), "HANDLER_ERROR", e.getMessage());
                }

                writeResponse(out, resp);
            }
        } catch (Exception e) {
            System.err.println("vexfn: fatal: " + e.getMessage());
        }
    }

    private static void writeResponse(PrintStream out, VexResponse resp) {
        try {
            out.println(MAPPER.writeValueAsString(resp));
            out.flush();
        } catch (Exception e) {
            System.err.println("vexfn: encode error: " + e.getMessage());
        }
    }
}
