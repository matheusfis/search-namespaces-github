package com.matheusfischer.githubnamespacescanner.domain.model;

import com.matheusfischer.githubnamespacescanner.domain.exception.ApplicationConfigurationException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubApiException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubApiUnavailableException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubAuthenticationException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubNotFoundException;
import com.matheusfischer.githubnamespacescanner.domain.exception.GitHubRateLimitException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainValueObjectsTest {

    @Test
    void shouldValidateAndRedactGitHubToken() {
        var token = new GitHubToken("  secret-token  ");

        assertThat(token.value()).isEqualTo("secret-token");
        assertThat(token).hasToString("GitHubToken[REDACTED]");
        assertThatThrownBy(() -> new GitHubToken(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GitHubToken(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldNormalizeRepositoryTopicWithoutChangingItsDisplayValue() {
        var topic = new RepositoryTopic("  BackStage  ");

        assertThat(topic.value()).isEqualTo("BackStage");
        assertThat(topic.normalized()).isEqualTo("backstage");
        assertThatThrownBy(() -> new RepositoryTopic(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RepositoryTopic(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldValidateProxyUrlAndApplyDefaultPorts() {
        var http = ProxyUrl.fromNullable("http://proxy.example.com").orElseThrow();
        var https = new ProxyUrl(URI.create("https://proxy.example.com:8443/"));

        assertThat(http.host()).isEqualTo("proxy.example.com");
        assertThat(http.port()).isEqualTo(80);
        assertThat(https.port()).isEqualTo(8443);
        assertThat(ProxyUrl.fromNullable(null)).isEmpty();
        assertThat(ProxyUrl.fromNullable(" ")).isEmpty();
        assertThatThrownBy(() -> ProxyUrl.fromNullable("proxy-without-scheme"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed PROXY_URL");
        assertThatThrownBy(() -> new ProxyUrl(URI.create("ftp://proxy.example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProxyUrl(URI.create("http://user@proxy.example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProxyUrl(URI.create("http://proxy.example.com/path")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProxyUrl(URI.create("http://proxy.example.com?x=1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleOptionalCertificatePath() {
        var certificate = CertificatePath.fromNullable(" certs/company.pem ").orElseThrow();

        assertThat(certificate.value()).isEqualTo(Path.of("certs/company.pem"));
        assertThat(CertificatePath.fromNullable(null)).isEmpty();
        assertThat(CertificatePath.fromNullable(" ")).isEmpty();
        assertThatThrownBy(() -> new CertificatePath(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldValidateAndNormalizeGitHubApiUrl() {
        var defaultUrl = GitHubApiUrl.fromNullable(null);
        var enterprise = GitHubApiUrl.fromNullable("https://github.example.com/api/v3/");

        assertThat(defaultUrl.value()).isEqualTo(URI.create(GitHubApiUrl.DEFAULT_URL));
        assertThat(enterprise.endpoint("/orgs/acme/repos"))
                .isEqualTo("https://github.example.com/api/v3/orgs/acme/repos");
        assertThatThrownBy(() -> GitHubApiUrl.fromNullable("not-a-url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed GITHUB_API_URL");
        assertThatThrownBy(() -> new GitHubApiUrl(URI.create("ftp://github.example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GitHubApiUrl(URI.create("https://api.github.com?q=x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateSimpleNamesAndNamespace() {
        assertThat(new OrganizationName(" acme ").value()).isEqualTo("acme");
        assertThat(new RepositoryName(" app ").value()).isEqualTo("app");
        assertThat(VariableName.fromNullable(null).value()).isEqualTo("NAMESPACE");
        assertThat(VariableName.fromNullable(" CUSTOM ").value()).isEqualTo("CUSTOM");
        assertThat(new NamespaceValue(" team-a ").value()).isEqualTo("team-a");

        assertThatThrownBy(() -> new OrganizationName(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RepositoryName(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VariableName(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NamespaceValue(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMatchNoProxyHosts() {
        var noProxy = NoProxyHosts.parse("localhost,127.0.0.1,*.internal.example.com,.corp.local");

        assertThat(noProxy.matches("LOCALHOST")).isTrue();
        assertThat(noProxy.matches("api.internal.example.com")).isTrue();
        assertThat(noProxy.matches("corp.local")).isTrue();
        assertThat(noProxy.matches("service.corp.local")).isTrue();
        assertThat(noProxy.matches("github.com")).isFalse();
        assertThat(noProxy.matches(null)).isFalse();
        assertThat(NoProxyHosts.parse(null).values()).isEmpty();
        assertThat(NoProxyHosts.parse("*").matches("anything.example")).isTrue();
    }

    @Test
    void shouldCreateRepositoryAndScanSettingsDefensively() {
        var topics = new ArrayList<>(List.of(new RepositoryTopic("one")));
        var repository = new GitHubRepository(new RepositoryName("repo"), topics, false, false);
        topics.clear();

        assertThat(repository.active()).isTrue();
        assertThat(repository.topics()).hasSize(1);
        assertThat(new GitHubRepository(new RepositoryName("archived"), List.of(), true, false).active())
                .isFalse();

        var criteria = criteria();
        var connection = connection();
        var settings = new ScannerSettings(criteria, connection);
        assertThat(settings.criteria()).isSameAs(criteria);
        assertThat(settings.connection()).isSameAs(connection);

        assertThatThrownBy(() -> new ScanCriteria(
                new OrganizationName("acme"), List.of(), new VariableName("NAMESPACE")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GitHubConnectionSettings(
                connection.token(), connection.apiUrl(), null, connection.noProxyHosts(), Optional.empty()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRepresentVariableLookupResults() {
        assertThat(VariableLookupResult.found("value"))
                .isEqualTo(new VariableLookupResult.Found("value"));
        assertThat(VariableLookupResult.missing())
                .isEqualTo(new VariableLookupResult.Missing());
        assertThatThrownBy(() -> VariableLookupResult.found(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldMakeScanResultDeeplyImmutableAndValidateCounters() {
        var namespace = new NamespaceValue("team-a");
        var repository = new RepositoryName("repo-a");
        var repositories = new LinkedHashSet<>(List.of(repository));
        var withoutVariable = new ArrayList<>(List.of(new RepositoryName("repo-b")));
        var errors = new ArrayList<>(List.of(new RepositoryScanError(
                new RepositoryName("repo-c"), "403 forbidden")));

        var result = new ScanResult(
                criteria(),
                3,
                3,
                1,
                Map.of(namespace, repositories),
                withoutVariable,
                errors
        );
        repositories.clear();
        withoutVariable.clear();
        errors.clear();

        assertThat(result.namespaces().get(namespace)).containsExactly(repository);
        assertThat(result.repositoriesWithoutVariable()).hasSize(1);
        assertThat(result.errors()).hasSize(1);
        assertThatThrownBy(() -> result.namespaces().put(namespace, Set.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new ScanResult(
                criteria(), -1, 0, 0, Map.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RepositoryScanError(repository, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExposeActionableGitHubExceptionDiagnostics() {
        var authentication = new GitHubAuthenticationException(
                "unauthorized", "/endpoint", "REQ-1", "check token");
        var notFound = new GitHubNotFoundException(
                "missing", "/endpoint", null, "check repository");
        var rateLimit = new GitHubRateLimitException(
                "limited", "/endpoint", "REQ-2", "wait");
        var unavailable = new GitHubApiUnavailableException(
                "down", "/endpoint", 503, "REQ-3", "retry", new RuntimeException("cause"));
        var generic = new GitHubApiException(
                "forbidden", "/endpoint", 403, null, "check access");

        assertThat(authentication.statusCode()).isEqualTo(401);
        assertThat(authentication.diagnosticMessage())
                .contains("endpoint=/endpoint", "status=401", "X-GitHub-Request-Id=REQ-1", "check token");
        assertThat(notFound.statusCode()).isEqualTo(404);
        assertThat(notFound.requestId()).isEqualTo("not-provided");
        assertThat(rateLimit.statusCode()).isEqualTo(403);
        assertThat(unavailable.statusCode()).isEqualTo(503);
        assertThat(unavailable).hasCauseInstanceOf(RuntimeException.class);
        assertThat(generic.diagnosticMessage()).contains("status=403");

        var configuration = new ApplicationConfigurationException(
                "invalid", new IllegalArgumentException("cause"));
        assertThat(configuration).hasMessage("invalid").hasCauseInstanceOf(IllegalArgumentException.class);
        assertThat(new ApplicationConfigurationException("missing")).hasMessage("missing");
    }

    private ScanCriteria criteria() {
        return new ScanCriteria(
                new OrganizationName("acme"),
                List.of(new RepositoryTopic("backstage")),
                new VariableName("NAMESPACE")
        );
    }

    private GitHubConnectionSettings connection() {
        return new GitHubConnectionSettings(
                new GitHubToken("token"),
                GitHubApiUrl.fromNullable(null),
                Optional.empty(),
                NoProxyHosts.parse(null),
                Optional.empty()
        );
    }

}
