package io.vextura.sdk.vexfn;

/**
 * Runtime context passed to each handler invocation.
 */
public final class Context {
    private final PrimitivesClient primitives;
    private final String tenantId;
    private final String traceId;

    Context(PrimitivesClient primitives, String tenantId, String traceId) {
        this.primitives = primitives;
        this.tenantId   = tenantId;
        this.traceId    = traceId;
    }

    /** Client for calling other vex-fn functions from within a handler. */
    public PrimitivesClient primitives() { return primitives; }

    /** Tenant ID injected by vex-executor. */
    public String tenantId() { return tenantId; }

    /** Trace ID for distributed tracing. */
    public String traceId() { return traceId; }
}
