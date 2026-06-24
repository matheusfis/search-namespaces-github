package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;

public sealed interface VariableLookupResult
        permits VariableLookupResult.Found, VariableLookupResult.Missing {

    record Found(String value) implements VariableLookupResult {
        public Found {
            value = Objects.requireNonNull(value, "Variable value must not be null");
        }
    }

    record Missing() implements VariableLookupResult {
    }

    static Found found(String value) {
        return new Found(value);
    }

    static Missing missing() {
        return new Missing();
    }
}
