package io.vextura;

/** Base exception for all Vextura SDK errors. */
public class VexException extends RuntimeException {
    private final int statusCode;

    public VexException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public VexException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }
}
