package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;

public record OrganizationName(String value) {

    public OrganizationName {
        value = Objects.requireNonNull(value, "Organization name must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Organization name must not be blank");
        }
    }
}
