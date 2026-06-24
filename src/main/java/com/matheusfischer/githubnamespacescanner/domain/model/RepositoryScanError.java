package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;

public record RepositoryScanError(RepositoryName repository, String reason) {

    public RepositoryScanError {
        repository = Objects.requireNonNull(repository, "Repository must not be null");
        reason = Objects.requireNonNull(reason, "Error reason must not be null").trim();
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Error reason must not be blank");
        }
    }
}
