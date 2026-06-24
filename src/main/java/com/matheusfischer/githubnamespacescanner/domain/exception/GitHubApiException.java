package com.matheusfischer.githubnamespacescanner.domain.exception;

import java.util.Objects;

public class GitHubApiException extends RuntimeException {

    private final String endpoint;
    private final int statusCode;
    private final String requestId;
    private final String troubleshooting;

    public GitHubApiException(
            String message,
            String endpoint,
            int statusCode,
            String requestId,
            String troubleshooting
    ) {
        this(message, endpoint, statusCode, requestId, troubleshooting, null);
    }

    public GitHubApiException(
            String message,
            String endpoint,
            int statusCode,
            String requestId,
            String troubleshooting,
            Throwable cause
    ) {
        super(message, cause);
        this.endpoint = requireText(endpoint, "endpoint");
        this.statusCode = statusCode;
        this.requestId = normalizeRequestId(requestId);
        this.troubleshooting = requireText(troubleshooting, "troubleshooting");
    }

    public String endpoint() {
        return endpoint;
    }

    public int statusCode() {
        return statusCode;
    }

    public String requestId() {
        return requestId;
    }

    public String troubleshooting() {
        return troubleshooting;
    }

    public String diagnosticMessage() {
        return "%s | endpoint=%s | status=%d | X-GitHub-Request-Id=%s | troubleshooting=%s"
                .formatted(getMessage(), endpoint, statusCode, requestId, troubleshooting);
    }

    private static String normalizeRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? "not-provided" : requestId.trim();
    }

    private static String requireText(String value, String field) {
        var normalized = Objects.requireNonNull(value, field + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
