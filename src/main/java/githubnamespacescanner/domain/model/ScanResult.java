package githubnamespacescanner.domain.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ScanResult(
        ScanCriteria criteria,
        int totalOrganizationRepositories,
        int analyzedRepositories,
        int repositoriesWithVariable,
        Map<NamespaceValue, Set<RepositoryName>> namespaces,
        List<RepositoryName> repositoriesWithoutVariable,
        List<RepositoryScanError> errors
) {

    public ScanResult {
        criteria = Objects.requireNonNull(criteria, "Scan criteria must not be null");
        if (totalOrganizationRepositories < 0 || analyzedRepositories < 0 || repositoriesWithVariable < 0) {
            throw new IllegalArgumentException("Repository counters must not be negative");
        }
        namespaces = immutableNamespaces(namespaces);
        repositoriesWithoutVariable = List.copyOf(Objects.requireNonNull(
                repositoriesWithoutVariable, "Repositories without variable must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "Errors must not be null"));
    }

    private static Map<NamespaceValue, Set<RepositoryName>> immutableNamespaces(
            Map<NamespaceValue, Set<RepositoryName>> source
    ) {
        Objects.requireNonNull(source, "Namespaces must not be null");
        var copy = new LinkedHashMap<NamespaceValue, Set<RepositoryName>>();
        source.forEach((namespace, repositories) -> copy.put(
                Objects.requireNonNull(namespace, "Namespace must not be null"),
                Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(
                        repositories, "Namespace repositories must not be null")))
        ));
        return Map.copyOf(copy);
    }
}
