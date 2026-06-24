package com.matheusfischer.githubnamespacescanner.domain.model;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ProxyUrl(URI value) {

    private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

    public ProxyUrl {
        value = Objects.requireNonNull(value, "Proxy URL must not be null");
        var scheme = Optional.ofNullable(value.getScheme())
                .map(candidate -> candidate.toLowerCase(Locale.ROOT))
                .orElse("");
        if (!SUPPORTED_SCHEMES.contains(scheme) || value.getHost() == null) {
            throw new IllegalArgumentException("Proxy URL must be an absolute HTTP or HTTPS URL");
        }
        if (value.getUserInfo() != null || value.getQuery() != null || value.getFragment() != null) {
            throw new IllegalArgumentException("Proxy URL must not contain credentials, query parameters or fragments");
        }
        if (value.getPath() != null && !value.getPath().isBlank() && !"/".equals(value.getPath())) {
            throw new IllegalArgumentException("Proxy URL must not contain a path");
        }
        if (value.getPort() < -1 || value.getPort() > 65_535) {
            throw new IllegalArgumentException("Proxy URL port is invalid");
        }
    }

    public static Optional<ProxyUrl> fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ProxyUrl(URI.create(rawValue.trim())));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Malformed PROXY_URL: " + rawValue, exception);
        }
    }

    public String host() {
        return value.getHost();
    }

    public int port() {
        if (value.getPort() >= 0) {
            return value.getPort();
        }
        return "https".equalsIgnoreCase(value.getScheme()) ? 443 : 80;
    }
}
