package githubnamespacescanner.domain.model;

import java.util.Objects;

public record NamespaceValue(String value) {

    public NamespaceValue {
        value = Objects.requireNonNull(value, "Namespace value must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Namespace value must not be blank");
        }
    }
}
