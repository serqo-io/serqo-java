package io.serqo.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/** API error carrying the HTTP status, request ID, and structured error body. */
public final class SerqoException extends RuntimeException {
    private final int status;
    private final String code;
    private final String requestId;
    private final boolean retryable;
    private final Integer retryAfterMs;
    private final JsonNode details;

    public SerqoException(int status, String code, String message, String requestId,
                          boolean retryable, Integer retryAfterMs, JsonNode details) {
        super(message);
        this.status = status;
        this.code = code;
        this.requestId = requestId;
        this.retryable = retryable;
        this.retryAfterMs = retryAfterMs;
        this.details = details;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getRequestId() { return requestId; }
    public boolean isRetryable() { return retryable; }
    public Integer getRetryAfterMs() { return retryAfterMs; }
    public JsonNode getDetails() { return details; }
}
