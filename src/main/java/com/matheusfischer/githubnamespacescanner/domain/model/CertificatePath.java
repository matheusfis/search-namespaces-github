package com.matheusfischer.githubnamespacescanner.domain.model;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record CertificatePath(Path value) {

    public CertificatePath {
        value = Objects.requireNonNull(value, "Certificate path must not be null").normalize();
        if (value.toString().isBlank()) {
            throw new IllegalArgumentException("Certificate path must not be blank");
        }
    }

    public static Optional<CertificatePath> fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CertificatePath(Path.of(rawValue.trim())));
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Malformed GITHUB_API_CERT_PATH: " + rawValue, exception);
        }
    }
}
