package com.matheusfischer.githubnamespacescanner.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubVariableResponse(String name, String value) {
}
