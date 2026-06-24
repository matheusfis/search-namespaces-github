package githubnamespacescanner.domain.exception;

public final class GitHubAuthenticationException extends GitHubApiException {

    public GitHubAuthenticationException(String message, String endpoint, String requestId, String troubleshooting) {
        super(message, endpoint, 401, requestId, troubleshooting);
    }
}
