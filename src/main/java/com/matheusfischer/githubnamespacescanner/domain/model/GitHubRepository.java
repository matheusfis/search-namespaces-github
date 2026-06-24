package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.List;
import java.util.Objects;

public record GitHubRepository(
        RepositoryName name,
        List<RepositoryTopic> topics,
        boolean archived,
        boolean disabled
) {

    public GitHubRepository {
        name = Objects.requireNonNull(name, "Repository name must not be null");
        topics = List.copyOf(Objects.requireNonNull(topics, "Repository topics must not be null"));
    }

    public boolean active() {
        return !archived && !disabled;
    }
}
