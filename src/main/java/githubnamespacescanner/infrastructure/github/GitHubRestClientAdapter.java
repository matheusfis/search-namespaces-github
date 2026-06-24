package githubnamespacescanner.infrastructure.github;

import githubnamespacescanner.domain.exception.GitHubNotFoundException;
import githubnamespacescanner.domain.model.GitHubConnectionSettings;
import githubnamespacescanner.domain.model.GitHubRepository;
import githubnamespacescanner.domain.model.OrganizationName;
import githubnamespacescanner.domain.model.RepositoryName;
import githubnamespacescanner.domain.model.RepositoryTopic;
import githubnamespacescanner.domain.model.VariableLookupResult;
import githubnamespacescanner.domain.model.VariableName;
import githubnamespacescanner.domain.port.GitHubRepositoryPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GitHubRestClientAdapter implements GitHubRepositoryPort {

    static final String API_VERSION = "2022-11-28";
    private static final int PAGE_SIZE = 100;

    private final RestTemplate restTemplate;
    private final GitHubConnectionSettings settings;

    public GitHubRestClientAdapter(RestTemplate restTemplate, GitHubConnectionSettings settings) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate must not be null");
        this.settings = Objects.requireNonNull(settings, "Connection settings must not be null");
    }

    @Override
    public List<GitHubRepository> findAllByOrganization(OrganizationName organization) {
        /*
         * DECISÃO DE API: GET /orgs/{org}/repos foi escolhido em vez de Search ou GraphQL
         * porque o comportamento original é limitado a uma organização, inclui repositórios
         * privados visíveis ao token, precisa contar todos os ativos e aplica vários tópicos
         * com AND localmente. O endpoint aceita 100 itens por página e não usa o rate limit
         * mais restritivo da Search API.
         */
        var repositories = new ArrayList<GitHubRepository>();
        var page = 1;
        List<GitHubRepositoryResponse> responsePage;

        do {
            var endpoint = UriComponentsBuilder.fromUriString(settings.apiUrl().value().toString())
                    .pathSegment("orgs", organization.value(), "repos")
                    .queryParam("type", "all")
                    .queryParam("per_page", PAGE_SIZE)
                    .queryParam("page", page)
                    .build()
                    .encode()
                    .toUri();

            responsePage = getList(endpoint);
            responsePage.stream().map(this::toDomain).forEach(repositories::add);
            page++;
        } while (responsePage.size() == PAGE_SIZE);

        return List.copyOf(repositories);
    }

    @Override
    public List<RepositoryTopic> findTopics(
            OrganizationName organization,
            RepositoryName repository
    ) {
        var endpoint = UriComponentsBuilder.fromUriString(settings.apiUrl().value().toString())
                .pathSegment("repos", organization.value(), repository.value(), "topics")
                .build()
                .encode()
                .toUri();
        var response = get(endpoint, GitHubTopicsResponse.class);
        return safeList(response.names()).stream()
                .filter(Objects::nonNull)
                .map(RepositoryTopic::new)
                .toList();
    }

    @Override
    public VariableLookupResult findVariable(
            OrganizationName organization,
            RepositoryName repository,
            VariableName variableName
    ) {
        /*
         * DECISÃO DE API: GET /repos/{owner}/{repo}/actions/variables/{name} consulta
         * somente a variável necessária em uma chamada. Listar todas as variáveis teria
         * payload maior sem reduzir o número de requests; GraphQL não oferece equivalente
         * para Actions Repository Variables.
         */
        var endpoint = UriComponentsBuilder.fromUriString(settings.apiUrl().value().toString())
                .pathSegment(
                        "repos",
                        organization.value(),
                        repository.value(),
                        "actions",
                        "variables",
                        variableName.value()
                )
                .build()
                .encode()
                .toUri();
        try {
            var response = get(endpoint, GitHubVariableResponse.class);
            return VariableLookupResult.found(response.value() == null ? "" : response.value());
        } catch (GitHubNotFoundException exception) {
            // Mantém o comportamento original: 404 nesta operação significa variável ausente.
            return VariableLookupResult.missing();
        }
    }

    private List<GitHubRepositoryResponse> getList(URI endpoint) {
        try {
            var response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    requestEntity(),
                    new ParameterizedTypeReference<List<GitHubRepositoryResponse>>() {
                    }
            );
            return safeList(response.getBody());
        } catch (RestClientResponseException exception) {
            throw GitHubHttpExceptionMapper.map(endpoint.toString(), exception);
        } catch (ResourceAccessException exception) {
            throw GitHubHttpExceptionMapper.map(endpoint.toString(), exception);
        }
    }

    private <T> T get(URI endpoint, Class<T> responseType) {
        try {
            var response = restTemplate.exchange(endpoint, HttpMethod.GET, requestEntity(), responseType);
            if (response.getBody() == null) {
                throw new ResourceAccessException("GitHub API returned an empty response body");
            }
            return response.getBody();
        } catch (RestClientResponseException exception) {
            throw GitHubHttpExceptionMapper.map(endpoint.toString(), exception);
        } catch (ResourceAccessException exception) {
            throw GitHubHttpExceptionMapper.map(endpoint.toString(), exception);
        }
    }

    private HttpEntity<Void> requestEntity() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(settings.token().value());
        headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", API_VERSION);
        return new HttpEntity<>(headers);
    }

    private GitHubRepository toDomain(GitHubRepositoryResponse response) {
        var topics = safeList(response.topics()).stream()
                .filter(Objects::nonNull)
                .map(RepositoryTopic::new)
                .toList();
        return new GitHubRepository(
                new RepositoryName(response.name()),
                topics,
                response.archived(),
                response.disabled()
        );
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
