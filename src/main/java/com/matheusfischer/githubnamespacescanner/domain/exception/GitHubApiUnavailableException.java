package com.matheusfischer.githubnamespacescanner.domain.exception;

public final class GitHubApiUnavailableException extends GitHubApiException {

    public GitHubApiUnavailableException(
            String message,
            String endpoint,
            int statusCode,
            String requestId,
            String troubleshooting,
            Throwable cause
    ) {
        super(message, endpoint, statusCode, requestId, troubleshooting, cause);
    }
}
