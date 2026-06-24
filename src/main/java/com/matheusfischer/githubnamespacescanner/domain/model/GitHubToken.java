package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;

public record GitHubToken(String value) {

    public GitHubToken {
        value = Objects.requireNonNull(value, "GitHub token must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("GitHub token must not be blank");
        }
    }

    @Override
    public String toString() {
        return "GitHubToken[REDACTED]";
    }
}
