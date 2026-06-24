package githubnamespacescanner.domain.model;

import java.util.List;
import java.util.Objects;

public record ScanCriteria(
        OrganizationName organization,
        List<RepositoryTopic> topics,
        VariableName variableName
) {

    public ScanCriteria {
        organization = Objects.requireNonNull(organization, "Organization must not be null");
        topics = List.copyOf(Objects.requireNonNull(topics, "Topics must not be null"));
        variableName = Objects.requireNonNull(variableName, "Variable name must not be null");
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("At least one repository topic is required");
        }
    }
}
