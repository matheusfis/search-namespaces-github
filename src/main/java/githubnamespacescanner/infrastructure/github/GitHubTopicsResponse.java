package githubnamespacescanner.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubTopicsResponse(List<String> names) {
}
