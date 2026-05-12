package io.vextura.sdk.vexfn;

/**
 * Implement this interface and pass it to {@link VexFn#run(VexFnHandler)}
 * to handle VexEdge events.
 */
@FunctionalInterface
public interface VexFnHandler {
    /**
     * Process one event.
     *
     * @param event the incoming event from vex-executor
     * @param ctx   runtime context (access to PrimitivesClient etc.)
     * @return response written back to vex-executor
     * @throws Exception any exception is caught and converted to an error response
     */
    VexResponse handle(VexEvent event, Context ctx) throws Exception;
}
