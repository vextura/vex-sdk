package io.vextura.sdk.vexfn;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls other vex-fn functions or vex-gate endpoints from within a running container.
 * The base URL is injected by vex-executor via the {@code VEX_PRIMITIVES_URL} env var.
 */
public final class PrimitivesClient {
    private final String baseUrl;
    private final HttpClient http;

    PrimitivesClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http    = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Invoke a named function and return the raw JSON response body.
     *
     * @param fnName  function name as registered in vex-registry
     * @param payload JSON string to POST as the request body
     * @return raw JSON response string
     */
    public String call(String fnName, String payload) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/fn/" + fnName))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IOException("primitives call " + fnName + " failed: HTTP " + resp.statusCode());
        }
        return resp.body();
    }
}
