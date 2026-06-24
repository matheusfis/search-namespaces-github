package githubnamespacescanner.domain.model;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record GitHubApiUrl(URI value) {

    public static final String DEFAULT_URL = "https://api.github.com";
    private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

    public GitHubApiUrl {
        value = normalize(Objects.requireNonNull(value, "GitHub API URL must not be null"));
        var scheme = value.getScheme() == null ? "" : value.getScheme().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SCHEMES.contains(scheme) || value.getHost() == null) {
            throw new IllegalArgumentException("GitHub API URL must be an absolute HTTP or HTTPS URL");
        }
        if (value.getQuery() != null || value.getFragment() != null) {
            throw new IllegalArgumentException("GitHub API URL must not contain query parameters or fragments");
        }
    }

    public static GitHubApiUrl fromNullable(String rawValue) {
        var candidate = rawValue == null || rawValue.isBlank() ? DEFAULT_URL : rawValue.trim();
        try {
            return new GitHubApiUrl(URI.create(candidate));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Malformed GITHUB_API_URL: " + candidate, exception);
        }
    }

    public String endpoint(String path) {
        var suffix = path.startsWith("/") ? path : "/" + path;
        return value + suffix;
    }

    private static URI normalize(URI uri) {
        var text = uri.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
    }
}
