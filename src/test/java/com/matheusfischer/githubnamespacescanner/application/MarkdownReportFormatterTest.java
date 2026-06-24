package com.matheusfischer.githubnamespacescanner.application;

import com.matheusfischer.githubnamespacescanner.domain.model.NamespaceValue;
import com.matheusfischer.githubnamespacescanner.domain.model.OrganizationName;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryName;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryScanError;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryTopic;
import com.matheusfischer.githubnamespacescanner.domain.model.ScanCriteria;
import com.matheusfischer.githubnamespacescanner.domain.model.ScanResult;
import com.matheusfischer.githubnamespacescanner.domain.model.VariableName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportFormatterTest {

    private final MarkdownReportFormatter formatter = new MarkdownReportFormatter();

    @Test
    void shouldFormatCompleteSortedReport() {
        var namespaces = new LinkedHashMap<NamespaceValue, Set<RepositoryName>>();
        namespaces.put(
                new NamespaceValue("z-team"),
                new LinkedHashSet<>(List.of(new RepositoryName("z-repo")))
        );
        namespaces.put(
                new NamespaceValue("a-team"),
                new LinkedHashSet<>(List.of(new RepositoryName("b-repo"), new RepositoryName("a-repo")))
        );
        var result = new ScanResult(
                criteria(),
                5,
                4,
                3,
                namespaces,
                List.of(new RepositoryName("without-var")),
                List.of(new RepositoryScanError(new RepositoryName("denied"), "403 denied"))
        );

        var report = formatter.format(result);

        assertThat(report)
                .contains("# Namespaces encontrados")
                .contains("Org: acme | Topics (AND): backstage, openshift")
                .contains("Repos analisados: 4 | Com NAMESPACE: 3 | Namespaces únicos: 2")
                .contains("## Repos sem a variável NAMESPACE")
                .contains("## Repos com erro de acesso (ignorados)")
                .contains("- denied: 403 denied");
        assertThat(report.indexOf("## a-team")).isLessThan(report.indexOf("## z-team"));
        assertThat(report.indexOf("- a-repo")).isLessThan(report.indexOf("- b-repo"));
    }

    @Test
    void shouldFormatNoMatchingRepositoryMessage() {
        var result = new ScanResult(criteria(), 3, 0, 0, Map.of(), List.of(), List.of());

        assertThat(formatter.format(result))
                .contains("Nenhum repositório da org **acme** possui todos os topics")
                .doesNotContain("Repos com erro de acesso");
    }

    @Test
    void shouldFormatNoVariableMessage() {
        var result = new ScanResult(
                criteria(),
                1,
                1,
                0,
                Map.of(),
                List.of(new RepositoryName("repo")),
                List.of()
        );

        assertThat(formatter.format(result))
                .contains("Repos selecionados pelos topics não possuem a variável NAMESPACE.")
                .contains("## Repos sem a variável NAMESPACE");
    }

    private ScanCriteria criteria() {
        return new ScanCriteria(
                new OrganizationName("acme"),
                List.of(new RepositoryTopic("backstage"), new RepositoryTopic("openshift")),
                new VariableName("NAMESPACE")
        );
    }
}
