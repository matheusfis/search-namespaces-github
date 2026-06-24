package com.matheusfischer.githubnamespacescanner.application;

import com.matheusfischer.githubnamespacescanner.domain.model.NamespaceValue;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryName;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryScanError;
import com.matheusfischer.githubnamespacescanner.domain.model.ScanResult;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public final class MarkdownReportFormatter {

    public String format(ScanResult result) {
        Objects.requireNonNull(result, "Scan result must not be null");
        var comparator = textComparator();
        var lines = new ArrayList<String>();
        var criteria = result.criteria();
        var topics = criteria.topics().stream()
                .map(topic -> topic.value())
                .toList();

        lines.add("# Namespaces encontrados");
        lines.add("");
        lines.add("Org: %s | Topics (AND): %s".formatted(
                criteria.organization().value(),
                String.join(", ", topics)
        ));
        lines.add("Repos analisados: %d | Com %s: %d | Namespaces únicos: %d".formatted(
                result.analyzedRepositories(),
                criteria.variableName().value(),
                result.repositoriesWithVariable(),
                result.namespaces().size()
        ));
        lines.add("");

        if (result.analyzedRepositories() == 0) {
            lines.add("> Nenhum repositório da org **%s** possui todos os topics informados (%s)."
                    .formatted(criteria.organization().value(), String.join(", ", topics)));
            lines.add("");
            return String.join("\n", lines);
        }

        var sortedNamespaces = result.namespaces().keySet().stream()
                .sorted(Comparator.comparing(NamespaceValue::value, comparator))
                .toList();

        if (sortedNamespaces.isEmpty()) {
            lines.add("> Repos selecionados pelos topics não possuem a variável %s."
                    .formatted(criteria.variableName().value()));
            lines.add("");
        }

        for (var namespace : sortedNamespaces) {
            lines.add("## " + namespace.value());
            result.namespaces().get(namespace).stream()
                    .map(RepositoryName::value)
                    .sorted(comparator)
                    .map(repository -> "- " + repository)
                    .forEach(lines::add);
            lines.add("");
        }

        if (!result.repositoriesWithoutVariable().isEmpty()) {
            lines.add("## Repos sem a variável " + criteria.variableName().value());
            result.repositoriesWithoutVariable().stream()
                    .map(RepositoryName::value)
                    .sorted(comparator)
                    .map(repository -> "- " + repository)
                    .forEach(lines::add);
            lines.add("");
        }

        if (!result.errors().isEmpty()) {
            lines.add("## Repos com erro de acesso (ignorados)");
            result.errors().stream()
                    .sorted(Comparator.comparing(
                            error -> error.repository().value(),
                            comparator
                    ))
                    .map(this::formatError)
                    .forEach(lines::add);
            lines.add("");
        }

        return String.join("\n", lines);
    }

    private String formatError(RepositoryScanError error) {
        return "- %s: %s".formatted(error.repository().value(), error.reason());
    }

    private Comparator<String> textComparator() {
        var collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        return collator::compare;
    }
}
