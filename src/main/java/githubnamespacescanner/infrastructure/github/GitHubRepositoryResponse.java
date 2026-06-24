package githubnamespacescanner.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubRepositoryResponse(
        String name,
        List<String> topics,
        boolean archived,
        boolean disabled
) {
}
