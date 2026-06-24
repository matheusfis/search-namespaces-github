package com.matheusfischer.githubnamespacescanner.domain.model;

import java.util.Objects;
import java.util.Optional;

public record GitHubConnectionSettings(
        GitHubToken token,
        GitHubApiUrl apiUrl,
        Optional<ProxyUrl> proxyUrl,
        NoProxyHosts noProxyHosts,
        Optional<CertificatePath> certificatePath
) {

    public GitHubConnectionSettings {
        token = Objects.requireNonNull(token, "GitHub token must not be null");
        apiUrl = Objects.requireNonNull(apiUrl, "GitHub API URL must not be null");
        proxyUrl = Objects.requireNonNull(proxyUrl, "Proxy URL optional must not be null");
        noProxyHosts = Objects.requireNonNull(noProxyHosts, "NO_PROXY hosts must not be null");
        certificatePath = Objects.requireNonNull(certificatePath, "Certificate path optional must not be null");
    }
}
