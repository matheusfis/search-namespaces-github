package githubnamespacescanner.domain.exception;

public final class GitHubNotFoundException extends GitHubApiException {

    public GitHubNotFoundException(String message, String endpoint, String requestId, String troubleshooting) {
        super(message, endpoint, 404, requestId, troubleshooting);
    }
}
