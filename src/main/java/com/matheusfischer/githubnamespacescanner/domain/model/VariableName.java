package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;

public record VariableName(String value) {

    public static final String DEFAULT_NAME = "NAMESPACE";

    public VariableName {
        value = Objects.requireNonNull(value, "Variable name must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Variable name must not be blank");
        }
    }

    public static VariableName fromNullable(String rawValue) {
        return new VariableName(rawValue == null || rawValue.isBlank() ? DEFAULT_NAME : rawValue);
    }
}
