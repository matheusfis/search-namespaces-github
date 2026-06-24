package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;

public record RepositoryName(String value) {

    public RepositoryName {
        value = Objects.requireNonNull(value, "Repository name must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
    }
}
