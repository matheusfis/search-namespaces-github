package com.matheusfischer.githubnamespacescanner.application;

import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubApiException;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubRepository;
import com.matheusfischer.githubnamespacescanner.domain.model.NamespaceValue;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryName;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryScanError;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryTopic;
import com.matheusfischer.githubnamespacescanner.domain.model.ScanCriteria;
import com.matheusfischer.githubnamespacescanner.domain.model.ScanResult;
import com.matheusfischer.githubnamespacescanner.domain.model.VariableLookupResult;
import com.matheusfischer.githubnamespacescanner.domain.port.GitHubRepositoryPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ScanRepositoriesUseCase {

    private final GitHubRepositoryPort gitHubRepositoryPort;

    public ScanRepositoriesUseCase(GitHubRepositoryPort gitHubRepositoryPort) {
        this.gitHubRepositoryPort = Objects.requireNonNull(
                gitHubRepositoryPort, "GitHub repository port must not be null");
    }

    public ScanResult execute(ScanCriteria criteria) {
        Objects.requireNonNull(criteria, "Scan criteria must not be null");

        var activeRepositories = gitHubRepositoryPort.findAllByOrganization(criteria.organization())
                .stream()
                .filter(GitHubRepository::active)
                .toList();

        var wantedTopics = criteria.topics().stream()
                .map(RepositoryTopic::normalized)
                .toList();
        var namespaces = new LinkedHashMap<NamespaceValue, Set<RepositoryName>>();
        var repositoriesWithoutVariable = new ArrayList<RepositoryName>();
        var errors = new ArrayList<RepositoryScanError>();
        var analyzedRepositories = 0;
        var repositoriesWithVariable = 0;

        for (var repository : activeRepositories) {
            var repositoryTopics = resolveTopics(criteria, repository);
            var normalizedTopics = repositoryTopics.stream()
                    .map(RepositoryTopic::normalized)
                    .collect(java.util.stream.Collectors.toSet());

            if (!normalizedTopics.containsAll(wantedTopics)) {
                continue;
            }

            analyzedRepositories++;
            try {
                var lookup = gitHubRepositoryPort.findVariable(
                        criteria.organization(),
                        repository.name(),
                        criteria.variableName()
                );

                switch (lookup) {
                    case VariableLookupResult.Missing ignored ->
                            repositoriesWithoutVariable.add(repository.name());
                    case VariableLookupResult.Found found -> {
                        var value = found.value().trim();
                        if (value.isEmpty()) {
                            repositoriesWithoutVariable.add(repository.name());
                            continue;
                        }
                        var namespace = new NamespaceValue(value);
                        namespaces.computeIfAbsent(namespace, ignored -> new LinkedHashSet<>())
                                .add(repository.name());
                        repositoriesWithVariable++;
                    }
                }
            } catch (GitHubApiException exception) {
                errors.add(new RepositoryScanError(repository.name(), exception.diagnosticMessage()));
            } catch (RuntimeException exception) {
                var message = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                errors.add(new RepositoryScanError(repository.name(), message));
            }
        }

        return new ScanResult(
                criteria,
                activeRepositories.size(),
                analyzedRepositories,
                repositoriesWithVariable,
                namespaces,
                repositoriesWithoutVariable,
                errors
        );
    }

    private List<RepositoryTopic> resolveTopics(ScanCriteria criteria, GitHubRepository repository) {
        if (!repository.topics().isEmpty()) {
            return repository.topics();
        }
        try {
            return gitHubRepositoryPort.findTopics(criteria.organization(), repository.name());
        } catch (GitHubApiException exception) {
            // Preserva o fallback original: falha ao buscar tópicos equivale a lista vazia.
            return List.of();
        }
    }
}
