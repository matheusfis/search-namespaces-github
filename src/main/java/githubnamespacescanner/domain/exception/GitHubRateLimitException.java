package githubnamespacescanner.domain.exception;

public final class GitHubRateLimitException extends GitHubApiException {

    public GitHubRateLimitException(String message, String endpoint, String requestId, String troubleshooting) {
        super(message, endpoint, 403, requestId, troubleshooting);
    }
}
