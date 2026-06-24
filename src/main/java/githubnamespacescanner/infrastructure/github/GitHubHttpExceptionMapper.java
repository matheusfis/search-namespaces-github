package githubnamespacescanner.infrastructure.github;

import githubnamespacescanner.domain.exception.GitHubApiException;
import githubnamespacescanner.domain.exception.GitHubApiUnavailableException;
import githubnamespacescanner.domain.exception.GitHubAuthenticationException;
import githubnamespacescanner.domain.exception.GitHubNotFoundException;
import githubnamespacescanner.domain.exception.GitHubRateLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

final class GitHubHttpExceptionMapper {

    private static final String REQUEST_ID_HEADER = "X-GitHub-Request-Id";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private GitHubHttpExceptionMapper() {
    }

    static GitHubApiException map(String endpoint, RestClientResponseException exception) {
        var status = exception.getStatusCode().value();
        var headers = exception.getResponseHeaders();
        var requestId = firstHeader(headers, REQUEST_ID_HEADER);
        var message = "GitHub API retornou HTTP %d (%s)".formatted(
                status,
                exception.getStatusText()
        );

        if (status == 403 && "0".equals(firstHeader(headers, RATE_LIMIT_REMAINING_HEADER))) {
            var reset = firstHeader(headers, RATE_LIMIT_RESET_HEADER);
            var rateLimitMessage = reset == null
                    ? message
                    : message + "; X-RateLimit-Reset=" + reset;
            return new GitHubRateLimitException(
                    rateLimitMessage,
                    endpoint,
                    requestId,
                    GitHubTroubleshootingGuide.RATE_LIMIT
            );
        }

        return switch (status) {
            case 401 -> new GitHubAuthenticationException(
                    message,
                    endpoint,
                    requestId,
                    GitHubTroubleshootingGuide.AUTHENTICATION
            );
            case 404 -> new GitHubNotFoundException(
                    message,
                    endpoint,
                    requestId,
                    GitHubTroubleshootingGuide.NOT_FOUND
            );
            case 500, 501, 502, 503, 504, 505, 506, 507, 508, 510, 511 ->
                    new GitHubApiUnavailableException(
                            message,
                            endpoint,
                            status,
                            requestId,
                            GitHubTroubleshootingGuide.UNAVAILABLE,
                            exception
                    );
            default -> new GitHubApiException(
                    message,
                    endpoint,
                    status,
                    requestId,
                    status == 403
                            ? GitHubTroubleshootingGuide.FORBIDDEN
                            : GitHubTroubleshootingGuide.GENERIC,
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
                GitHubTroubleshootingGuide.UNAVAILABLE,
                exception
        );
    }

    private static String firstHeader(HttpHeaders headers, String name) {
        return headers == null ? null : headers.getFirst(name);
    }
}
