package com.matheusfischer.githubnamespacescanner.infrastructure.configuration;

import com.matheusfischer.githubnamespacescanner.domain.exception.ApplicationConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DotenvScannerSettingsLoaderTest {

    private final DotenvScannerSettingsLoader loader = new DotenvScannerSettingsLoader();

    @Test
    void shouldLoadRequiredValuesWithoutOptionalNetworkConfiguration() {
        var environment = requiredEnvironment();

        var settings = loader.load(new String[0], environment::get);

        assertThat(settings.criteria().organization().value()).isEqualTo("acme");
        assertThat(settings.criteria().topics())
                .extracting(topic -> topic.value())
                .containsExactly("backstage", "openshift");
        assertThat(settings.criteria().variableName().value()).isEqualTo("NAMESPACE");
        assertThat(settings.connection().proxyUrl()).isEmpty();
        assertThat(settings.connection().certificatePath()).isEmpty();
        assertThat(settings.connection().noProxyHosts().values()).isEmpty();
    }

    @Test
    void shouldApplyCliOverridesAndOptionalNetworkValues() {
        var environment = requiredEnvironment();
        environment.put("PROXY_URL", "http://proxy.example.com:8080");
        environment.put("NO_PROXY", "localhost,127.0.0.1");
        environment.put("GITHUB_API_CERT_PATH", "company.pem");
        environment.put("VAR_NAME", "CUSTOM_NAMESPACE");

        var settings = loader.load(
                new String[]{"--org", "cli-org", "--topics=one,two", "--ignored"},
                environment::get
        );

        assertThat(settings.criteria().organization().value()).isEqualTo("cli-org");
        assertThat(settings.criteria().topics())
                .extracting(topic -> topic.value())
                .containsExactly("one", "two");
        assertThat(settings.criteria().variableName().value()).isEqualTo("CUSTOM_NAMESPACE");
        assertThat(settings.connection().proxyUrl()).isPresent();
        assertThat(settings.connection().certificatePath()).isPresent();
        assertThat(settings.connection().noProxyHosts().matches("localhost")).isTrue();
    }

    @Test
    void shouldReportMissingAndMalformedConfiguration() {
        var missingToken = requiredEnvironment();
        missingToken.remove("GITHUB_TOKEN");

        assertThatThrownBy(() -> loader.load(new String[0], missingToken::get))
                .isInstanceOf(ApplicationConfigurationException.class)
                .hasMessageContaining("GITHUB_TOKEN");
        assertThatThrownBy(() -> loader.load(
                new String[]{"--org"},
                requiredEnvironment()::get
        ))
                .isInstanceOf(ApplicationConfigurationException.class)
                .hasMessageContaining("exige um valor");

        var malformedProxy = requiredEnvironment();
        malformedProxy.put("PROXY_URL", "not-a-url");
        assertThatThrownBy(() -> loader.load(new String[0], malformedProxy::get))
                .isInstanceOf(ApplicationConfigurationException.class)
                .hasMessageContaining("Malformed PROXY_URL");
    }

    private Map<String, String> requiredEnvironment() {
        var environment = new HashMap<String, String>();
        environment.put("GITHUB_TOKEN", "token");
        environment.put("GITHUB_ORG", "acme");
        environment.put("TOPICS", "backstage, openshift");
        return environment;
    }
}
