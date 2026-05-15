package io.vextura.auth;

import org.apache.hc.core5.http.HttpRequest;

/**
 * Auth strategy — applied to every outgoing HTTP request.
 *
 * <p>Default for Vextura: {@link #licenseKey(String)} — maps directly to
 * the license key issued by the Vextura portal (VX-PRO-XXXX-XXXX).
 *
 * <p>Secondary: {@link #bearer(String)} for banks with existing JWT infrastructure.
 */
public interface VexAuth {

    void applyTo(HttpRequest request);

    /** License key auth — recommended default. Sent as X-Vextura-License header. */
    static VexAuth licenseKey(String key) {
        return request -> request.setHeader("X-Vextura-License", key);
    }

    /** Bearer token auth — for JWT/OIDC integration. */
    static VexAuth bearer(String token) {
        return request -> request.setHeader("Authorization", "Bearer " + token);
    }

    /** No auth — only for local development / open engine. */
    static VexAuth none() {
        return request -> {};
    }
}
