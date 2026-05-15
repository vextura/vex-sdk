package io.vextura;

public class VexAuthException extends VexException {
    public VexAuthException() {
        super("Invalid or missing Vextura license key. Check your portal at https://portal.vextura.io", 401);
    }
}
