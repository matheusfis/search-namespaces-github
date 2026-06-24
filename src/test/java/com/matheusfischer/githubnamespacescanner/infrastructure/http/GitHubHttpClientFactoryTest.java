package com.matheusfischer.githubnamespacescanner.infrastructure.http;

import com.matheusfischer.githubnamespacescanner.domain.exception.ApplicationConfigurationException;
import com.matheusfischer.githubnamespacescanner.domain.model.CertificatePath;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubApiUrl;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubConnectionSettings;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubToken;
import com.matheusfischer.githubnamespacescanner.domain.model.NoProxyHosts;
import com.matheusfischer.githubnamespacescanner.domain.model.ProxyUrl;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubHttpClientFactoryTest {

    private final List<HttpServer> servers = new ArrayList<>();
    private final GitHubHttpClientFactory factory = new GitHubHttpClientFactory();

    @AfterEach
    void stopServers() {
        servers.forEach(server -> server.stop(0));
    }

    @Test
    void shouldRouteRequestThroughConfiguredProxy() throws IOException {
        var proxyHits = new AtomicInteger();
        var proxy = server("proxied", proxyHits);
        var proxyUrl = "http://localhost:" + proxy.getAddress().getPort();
        var client = factory.create(settings(
                ProxyUrl.fromNullable(proxyUrl),
                NoProxyHosts.parse(null),
                Optional.empty()
        ));

        var response = client.getForObject("http://target.invalid/resource", String.class);

        assertThat(response).isEqualTo("proxied");
        assertThat(proxyHits).hasValue(1);
    }

    @Test
    void shouldConnectDirectlyWhenProxyIsEmpty() throws IOException {
        var directHits = new AtomicInteger();
        var direct = server("direct", directHits);
        var client = factory.create(settings(
                Optional.empty(),
                NoProxyHosts.parse(null),
                Optional.empty()
        ));

        var response = client.getForObject(
                "http://localhost:" + direct.getAddress().getPort() + "/resource",
                String.class
        );

        assertThat(response).isEqualTo("direct");
        assertThat(directHits).hasValue(1);
    }

    @Test
    void shouldBypassProxyForNoProxyHost() throws IOException {
        var directHits = new AtomicInteger();
        var proxyHits = new AtomicInteger();
        var direct = server("direct", directHits);
        var proxy = server("proxy", proxyHits);
        var client = factory.create(settings(
                ProxyUrl.fromNullable("http://localhost:" + proxy.getAddress().getPort()),
                NoProxyHosts.parse("localhost"),
                Optional.empty()
        ));

        var response = client.getForObject(
                "http://localhost:" + direct.getAddress().getPort() + "/resource",
                String.class
        );

        assertThat(response).isEqualTo("direct");
        assertThat(directHits).hasValue(1);
        assertThat(proxyHits).hasValue(0);
    }

    @Test
    void shouldRejectUnreadableCertificateFile() {
        var settings = settings(
                Optional.empty(),
                NoProxyHosts.parse(null),
                Optional.of(new CertificatePath(Path.of("missing-company-ca.pem")))
        );

        assertThatThrownBy(() -> factory.create(settings))
                .isInstanceOf(ApplicationConfigurationException.class)
                .hasMessageContaining("readable certificate file");
    }

    private HttpServer server(String body, AtomicInteger hits) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            var response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        servers.add(server);
        return server;
    }

    private GitHubConnectionSettings settings(
            Optional<ProxyUrl> proxyUrl,
            NoProxyHosts noProxyHosts,
            Optional<CertificatePath> certificatePath
    ) {
        return new GitHubConnectionSettings(
                new GitHubToken("token"),
                new GitHubApiUrl(URI.create("https://api.github.com")),
                proxyUrl,
                noProxyHosts,
                certificatePath
        );
    }
}
