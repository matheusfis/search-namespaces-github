package githubnamespacescanner.infrastructure.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import githubnamespacescanner.domain.exception.GitHubApiUnavailableException;
import githubnamespacescanner.domain.exception.GitHubAuthenticationException;
import githubnamespacescanner.domain.exception.GitHubNotFoundException;
import githubnamespacescanner.domain.exception.GitHubRateLimitException;
import githubnamespacescanner.domain.model.GitHubApiUrl;
import githubnamespacescanner.domain.model.GitHubConnectionSettings;
import githubnamespacescanner.domain.model.GitHubToken;
import githubnamespacescanner.domain.model.NoProxyHosts;
import githubnamespacescanner.domain.model.OrganizationName;
import githubnamespacescanner.domain.model.RepositoryName;
import githubnamespacescanner.domain.model.VariableLookupResult;
import githubnamespacescanner.domain.model.VariableName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubRestClientAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private GitHubRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        adapter = new GitHubRestClientAdapter(restTemplate, settings());
    }

    @Test
    void shouldPaginateOrganizationRepositoriesWithRequiredHeaders() throws JsonProcessingException {
        var firstPage = IntStream.range(0, 100)
                .mapToObj(index -> Map.of(
                        "name", "repo-" + index,
                        "topics", List.of("backstage"),
                        "archived", false,
                        "disabled", false
                ))
                .toList();
        server.expect(once(), requestTo(
                        "https://api.github.com/orgs/acme/repos?type=all&per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", GitHubRestClientAdapter.API_VERSION))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(firstPage),
                        MediaType.APPLICATION_JSON
                ));
        server.expect(once(), requestTo(
                        "https://api.github.com/orgs/acme/repos?type=all&per_page=100&page=2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var repositories = adapter.findAllByOrganization(new OrganizationName("acme"));

        assertThat(repositories).hasSize(100);
        assertThat(repositories.getFirst().name().value()).isEqualTo("repo-0");
        assertThat(repositories.getFirst().topics().getFirst().value()).isEqualTo("backstage");
        server.verify();
    }

    @Test
    void shouldReadFallbackTopicsAndSpecificVariable() {
        server.expect(requestTo("https://api.github.com/repos/acme/app/topics"))
                .andRespond(withSuccess("""
                        {"names":["backstage","openshift"]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.github.com/repos/acme/app/actions/variables/NAMESPACE"))
                .andRespond(withSuccess("""
                        {"name":"NAMESPACE","value":"team-a"}
                        """, MediaType.APPLICATION_JSON));

        var topics = adapter.findTopics(new OrganizationName("acme"), new RepositoryName("app"));
        var variable = adapter.findVariable(
                new OrganizationName("acme"),
                new RepositoryName("app"),
                new VariableName("NAMESPACE")
        );

        assertThat(topics).extracting(topic -> topic.value())
                .containsExactly("backstage", "openshift");
        assertThat(variable).isEqualTo(VariableLookupResult.found("team-a"));
        server.verify();
    }

    @Test
    void shouldTreatVariable404AsMissing() {
        server.expect(requestTo(
                        "https://api.github.com/repos/acme/app/actions/variables/NAMESPACE"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .header("X-GitHub-Request-Id", "REQ-MISSING"));

        var result = adapter.findVariable(
                new OrganizationName("acme"),
                new RepositoryName("app"),
                new VariableName("NAMESPACE")
        );

        assertThat(result).isEqualTo(VariableLookupResult.missing());
    }

    @Test
    void shouldMap401WithRequestIdAndTroubleshooting() {
        expectRepositoryError(HttpStatus.UNAUTHORIZED, "REQ-401", null);

        assertThatThrownBy(() -> adapter.findAllByOrganization(new OrganizationName("acme")))
                .isInstanceOf(GitHubAuthenticationException.class)
                .satisfies(throwable -> {
                    var exception = (GitHubAuthenticationException) throwable;
                    assertThat(exception.endpoint()).contains("/orgs/acme/repos");
                    assertThat(exception.requestId()).isEqualTo("REQ-401");
                    assertThat(exception.troubleshooting())
                            .contains("GITHUB_TOKEN", "repo", "read:org", "SSO", "1 hora");
                });
    }

    @Test
    void shouldMap404ForOrganizationListing() {
        expectRepositoryError(HttpStatus.NOT_FOUND, "REQ-404", null);

        assertThatThrownBy(() -> adapter.findAllByOrganization(new OrganizationName("acme")))
                .isInstanceOf(GitHubNotFoundException.class)
                .satisfies(throwable -> assertThat(
                        ((GitHubNotFoundException) throwable).troubleshooting())
                        .contains("case-sensitive", "repo-level", "org-level"));
    }

    @Test
    void shouldMapExhaustedRateLimit() {
        expectRepositoryError(HttpStatus.FORBIDDEN, "REQ-RATE", "0");

        assertThatThrownBy(() -> adapter.findAllByOrganization(new OrganizationName("acme")))
                .isInstanceOf(GitHubRateLimitException.class)
                .satisfies(throwable -> assertThat(throwable.getMessage())
                        .contains("X-RateLimit-Reset=1893456000"));
    }

    @Test
    void shouldMapServerFailure() {
        expectRepositoryError(HttpStatus.SERVICE_UNAVAILABLE, "REQ-503", null);

        assertThatThrownBy(() -> adapter.findAllByOrganization(new OrganizationName("acme")))
                .isInstanceOf(GitHubApiUnavailableException.class)
                .satisfies(throwable -> assertThat(
                        ((GitHubApiUnavailableException) throwable).statusCode()).isEqualTo(503));
    }

    private void expectRepositoryError(HttpStatus status, String requestId, String remaining) {
        var response = withStatus(status)
                .header("X-GitHub-Request-Id", requestId);
        if (remaining != null) {
            response = response
                    .header("X-RateLimit-Remaining", remaining)
                    .header("X-RateLimit-Reset", "1893456000");
        }
        server.expect(requestTo(
                        "https://api.github.com/orgs/acme/repos?type=all&per_page=100&page=1"))
                .andRespond(response);
    }

    private GitHubConnectionSettings settings() {
        return new GitHubConnectionSettings(
                new GitHubToken("token"),
                GitHubApiUrl.fromNullable(null),
                Optional.empty(),
                NoProxyHosts.parse(null),
                Optional.empty()
        );
    }
}
