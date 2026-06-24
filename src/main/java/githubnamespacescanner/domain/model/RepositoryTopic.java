package githubnamespacescanner.domain.model;

import java.util.Locale;
import java.util.Objects;

public record RepositoryTopic(String value) {

    public RepositoryTopic {
        value = Objects.requireNonNull(value, "Repository topic must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Repository topic must not be blank");
        }
    }

    public String normalized() {
        return value.toLowerCase(Locale.ROOT);
    }
}
