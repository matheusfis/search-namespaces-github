package githubnamespacescanner.application;

import githubnamespacescanner.domain.exception.GitHubAuthenticationException;
import githubnamespacescanner.domain.exception.GitHubNotFoundException;
import githubnamespacescanner.domain.exception.GitHubRateLimitException;
import githubnamespacescanner.domain.model.GitHubRepository;
import githubnamespacescanner.domain.model.NamespaceValue;
import githubnamespacescanner.domain.model.OrganizationName;
import githubnamespacescanner.domain.model.RepositoryName;
import githubnamespacescanner.domain.model.RepositoryTopic;
import githubnamespacescanner.domain.model.ScanCriteria;
import githubnamespacescanner.domain.model.VariableLookupResult;
import githubnamespacescanner.domain.model.VariableName;
import githubnamespacescanner.domain.port.GitHubRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanRepositoriesUseCaseTest {

    @Mock
    private GitHubRepositoryPort gitHub;

    private ScanRepositoriesUseCase useCase;
    private ScanCriteria criteria;

    @BeforeEach
    void setUp() {
        useCase = new ScanRepositoriesUseCase(gitHub);
        criteria = new ScanCriteria(
                new OrganizationName("acme"),
                List.of(new RepositoryTopic("BackStage"), new RepositoryTopic("OpenShift")),
                new VariableName("NAMESPACE")
        );
    }

    @Test
    void shouldScanActiveRepositoriesUsingAndTopicFilterAndFallbackTopics() {
        var inline = repository("inline", List.of("backstage", "OPENSHIFT"), false, false);
        var fallback = repository("fallback", List.of(), false, false);
        var archived = repository("archived", List.of("backstage", "openshift"), true, false);
        var disabled = repository("disabled", List.of("backstage", "openshift"), false, true);
        when(gitHub.findAllByOrganization(criteria.organization()))
                .thenReturn(List.of(inline, fallback, archived, disabled));
        when(gitHub.findTopics(criteria.organization(), fallback.name()))
                .thenReturn(topics("backstage", "openshift"));
        when(gitHub.findVariable(criteria.organization(), inline.name(), criteria.variableName()))
                .thenReturn(VariableLookupResult.found(" team-a "));
        when(gitHub.findVariable(criteria.organization(), fallback.name(), criteria.variableName()))
                .thenReturn(VariableLookupResult.found("team-a"));

        var result = useCase.execute(criteria);

        assertThat(result.totalOrganizationRepositories()).isEqualTo(2);
        assertThat(result.analyzedRepositories()).isEqualTo(2);
        assertThat(result.repositoriesWithVariable()).isEqualTo(2);
        assertThat(result.namespaces().get(new NamespaceValue("team-a")))
                .extracting(RepositoryName::value)
                .containsExactlyInAnyOrder("inline", "fallback");
        assertThat(result.repositoriesWithoutVariable()).isEmpty();
        assertThat(result.errors()).isEmpty();
        verify(gitHub, never()).findTopics(criteria.organization(), inline.name());
    }

    @Test
    void shouldPropagateGlobalAuthenticationError() {
        var exception = new GitHubAuthenticationException(
                "unauthorized", "/orgs/acme/repos", "REQ-401", "check token");
        when(gitHub.findAllByOrganization(criteria.organization())).thenThrow(exception);

        assertThatThrownBy(() -> useCase.execute(criteria))
                .isSameAs(exception);
    }

    @Test
    void shouldPropagateGlobalNotFoundError() {
        var exception = new GitHubNotFoundException(
                "missing", "/orgs/acme/repos", "REQ-404", "check organization");
        when(gitHub.findAllByOrganization(criteria.organization())).thenThrow(exception);

        assertThatThrownBy(() -> useCase.execute(criteria))
                .isSameAs(exception);
    }

    @Test
    void shouldRecordRateLimitErrorForOneRepositoryAndContinue() {
        var repository = repository("limited", List.of("backstage", "openshift"), false, false);
        when(gitHub.findAllByOrganization(criteria.organization())).thenReturn(List.of(repository));
        when(gitHub.findVariable(criteria.organization(), repository.name(), criteria.variableName()))
                .thenThrow(new GitHubRateLimitException(
                        "limited", "/variables/NAMESPACE", "REQ-RATE", "wait"));

        var result = useCase.execute(criteria);

        assertThat(result.analyzedRepositories()).isEqualTo(1);
        assertThat(result.repositoriesWithVariable()).isZero();
        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.reason())
                        .contains("status=403", "REQ-RATE", "wait"));
    }

    @Test
    void shouldTreatMissingAndBlankVariablesAsRepositoriesWithoutVariable() {
        var missing = repository("missing", List.of("backstage", "openshift"), false, false);
        var blank = repository("blank", List.of("backstage", "openshift"), false, false);
        when(gitHub.findAllByOrganization(criteria.organization())).thenReturn(List.of(missing, blank));
        when(gitHub.findVariable(criteria.organization(), missing.name(), criteria.variableName()))
                .thenReturn(VariableLookupResult.missing());
        when(gitHub.findVariable(criteria.organization(), blank.name(), criteria.variableName()))
                .thenReturn(VariableLookupResult.found("   "));

        var result = useCase.execute(criteria);

        assertThat(result.repositoriesWithoutVariable())
                .extracting(RepositoryName::value)
                .containsExactly("missing", "blank");
        assertThat(result.namespaces()).isEmpty();
    }

    @Test
    void shouldIgnoreRepositoryWhenTopicFallbackFails() {
        var repository = repository("no-topics", List.of(), false, false);
        when(gitHub.findAllByOrganization(criteria.organization())).thenReturn(List.of(repository));
        when(gitHub.findTopics(criteria.organization(), repository.name()))
                .thenThrow(new GitHubNotFoundException(
                        "missing", "/topics", "REQ-TOPICS", "check repository"));

        var result = useCase.execute(criteria);

        assertThat(result.analyzedRepositories()).isZero();
        verify(gitHub, never()).findVariable(
                criteria.organization(), repository.name(), criteria.variableName());
    }

    @Test
    void shouldRecordUnexpectedRepositoryErrorAndContinue() {
        var repository = repository("broken", List.of("backstage", "openshift"), false, false);
        when(gitHub.findAllByOrganization(criteria.organization())).thenReturn(List.of(repository));
        when(gitHub.findVariable(criteria.organization(), repository.name(), criteria.variableName()))
                .thenThrow(new IllegalStateException("unexpected mapping failure"));

        var result = useCase.execute(criteria);

        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.reason()).isEqualTo("unexpected mapping failure"));
    }

    private GitHubRepository repository(
            String name,
            List<String> topics,
            boolean archived,
            boolean disabled
    ) {
        return new GitHubRepository(
                new RepositoryName(name),
                topics.stream().map(RepositoryTopic::new).toList(),
                archived,
                disabled
        );
    }

    private List<RepositoryTopic> topics(String... values) {
        return java.util.Arrays.stream(values).map(RepositoryTopic::new).toList();
    }
}
