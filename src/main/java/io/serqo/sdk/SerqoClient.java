package io.serqo.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Small Java client for the public Serqo Search API. */
public final class SerqoClient {
    private static final String DEFAULT_BASE_URL = "https://api.serqo.io";
    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    /** Create a client with a 60-second request timeout. Keep the key server-side. */
    public SerqoClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, Duration.ofSeconds(60));
    }

    /** Create a client with a custom API origin and per-request timeout. */
    public SerqoClient(String apiKey, String baseUrl, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("apiKey is required");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();
    }

    /** Run one search. Pass an idempotency key only when retrying the same query. */
    public JsonNode search(Map<String, ?> query) {
        return search(query, null);
    }

    /** Run one search with an optional idempotency key for a safe retry. */
    public JsonNode search(Map<String, ?> query, String idempotencyKey) {
        return request("POST", "/v1/search", query, true, idempotencyKey);
    }

    /** Return the current engine and search-type capability list. */
    public JsonNode engines() { return request("GET", "/v1/engines", null, true, null); }

    /** Return the account and key identifiers associated with this API key. */
    public JsonNode me() { return request("GET", "/v1/me", null, true, null); }

    /** Check the product API process without sending an API key. */
    public JsonNode health() { return request("GET", "/v1/health", null, false, null); }

    private JsonNode request(String method, String path, Object body, boolean authenticated, String idempotencyKey) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(timeout).header("Accept", "application/json");
            if (authenticated) builder.header("x-api-key", apiKey);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) builder.header("Idempotency-Key", idempotencyKey);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode payload = mapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                JsonNode error = payload.path("error");
                String requestId = payload.path("requestId").asText(response.headers().firstValue("x-request-id").orElse(null));
                Integer retryAfter = error.has("retryAfterMs") ? error.path("retryAfterMs").asInt() : null;
                throw new SerqoException(response.statusCode(), error.path("code").asText("UNKNOWN_ERROR"),
                        error.path("message").asText("Serqo API request failed"), requestId,
                        error.path("retryable").asBoolean(false), retryAfter,
                        error.has("details") ? error.path("details") : null);
            }
            if (!payload.path("success").asBoolean(false)) {
                throw new IllegalStateException("Serqo API returned an unsuccessful response");
            }
            return payload;
        } catch (IOException error) {
            throw new IllegalStateException("Could not read the Serqo API response", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Serqo API request was interrupted", error);
        }
    }
}
