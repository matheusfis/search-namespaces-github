package githubnamespacescanner.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record NoProxyHosts(List<String> values) {

    public NoProxyHosts {
        values = List.copyOf(Objects.requireNonNull(values, "NO_PROXY hosts must not be null").stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList());
    }

    public static NoProxyHosts parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new NoProxyHosts(List.of());
        }
        return new NoProxyHosts(Arrays.asList(rawValue.split(",")));
    }

    public boolean matches(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        var normalizedHost = host.toLowerCase(Locale.ROOT);
        return values.stream().anyMatch(pattern -> matches(pattern, normalizedHost));
    }

    private boolean matches(String pattern, String host) {
        if ("*".equals(pattern) || pattern.equals(host)) {
            return true;
        }
        var suffix = pattern.startsWith("*.") ? pattern.substring(1) : pattern;
        if (suffix.startsWith(".")) {
            return host.endsWith(suffix) || host.equals(suffix.substring(1));
        }
        return false;
    }
}
