package io.vextura;

public class VexNotFoundException extends VexException {
    public VexNotFoundException(String message) { super(message, 404); }
}
