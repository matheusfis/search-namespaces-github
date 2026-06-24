package com.matheusfischer.githubnamespacescanner.infrastructure.github;

import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubApiException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubApiUnavailableException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubAuthenticationException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubNotFoundException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubRateLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

final class GitHubHttpExceptionMapper {

    private static final String REQUEST_ID_HEADER = "X-GitHub-Request-Id";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    private GitHubHttpExceptionMapper() {
    }

    static GitHubApiException map(String endpoint, RestClientResponseException exception) {
        var status = exception.getStatusCode().value();
        var headers = exception.getResponseHeaders();
        var requestId = firstHeader(headers, REQUEST_ID_HEADER);
        var message = "GitHub API request failed: " + exception.getStatusText();

        if (status == 403 && "0".equals(firstHeader(headers, RATE_LIMIT_REMAINING_HEADER))) {
            return new GitHubRateLimitException(
                    message, endpoint, requestId, "Wait for the GitHub rate-limit window to reset.");
        }

        return switch (status) {
            case 401 -> new GitHubAuthenticationException(
                    message, endpoint, requestId, "Verify the GitHub token and its permissions.");
            case 404 -> new GitHubNotFoundException(
                    message, endpoint, requestId, "Verify the resource name and token access.");
            case 500, 501, 502, 503, 504, 505, 506, 507, 508, 510, 511 ->
                    new GitHubApiUnavailableException(
                            message,
                            endpoint,
                            status,
                            requestId,
                            "Retry later and check GitHub status.",
                            exception
                    );
            default -> new GitHubApiException(
                    message,
                    endpoint,
                    status,
                    requestId,
                    "Review the GitHub API response and token permissions.",
                    exception
            );
        };
    }

    static GitHubApiUnavailableException map(String endpoint, ResourceAccessException exception) {
        return new GitHubApiUnavailableException(
                "GitHub API could not be reached",
                endpoint,
                0,
                null,
                "Check network, proxy and custom CA certificate configuration.",
                exception
        );
    }

    private static String firstHeader(HttpHeaders headers, String name) {
        return headers == null ? null : headers.getFirst(name);
    }
}
