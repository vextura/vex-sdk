package io.vextura;

public class VexExecutionTimeoutException extends VexException {
    public VexExecutionTimeoutException(String runId, long timeoutMs) {
        super("Execution " + runId + " did not complete within " + timeoutMs + "ms", 408);
    }
}
