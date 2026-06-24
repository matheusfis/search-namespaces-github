package com.matheusfischer.githubnamespacescanner.infrastructure.configuration;

import com.matheusfischer.githubnamespacescanner.domain.exception.ApplicationConfigurationException;
import com.matheusfischer.githubnamespacescanner.domain.model.CertificatePath;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubApiUrl;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubConnectionSettings;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubToken;
import com.matheusfischer.githubnamespacescanner.domain.model.NoProxyHosts;
import com.matheusfischer.githubnamespacescanner.domain.model.OrganizationName;
import com.matheusfischer.githubnamespacescanner.domain.model.ProxyUrl;
import com.matheusfischer.githubnamespacescanner.domain.model.RepositoryTopic;
import com.matheusfischer.githubnamespacescanner.domain.model.ScanCriteria;
import com.matheusfischer.githubnamespacescanner.domain.model.ScannerSettings;
import com.matheusfischer.githubnamespacescanner.domain.model.VariableName;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DotenvScannerSettingsLoader {

    public ScannerSettings load(String[] arguments) {
        var dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        return load(arguments, key -> {
            var environmentValue = System.getenv(key);
            return environmentValue != null ? environmentValue : dotenv.get(key);
        });
    }

    ScannerSettings load(String[] arguments, Function<String, String> environment) {
        try {
            var overrides = parseOverrides(arguments);
            var token = required(environment.apply("GITHUB_TOKEN"), "GITHUB_TOKEN");
            var organization = overrides.getOrDefault("org", environment.apply("GITHUB_ORG"));
            var topics = overrides.getOrDefault("topics", environment.apply("TOPICS"));

            var criteria = new ScanCriteria(
                    new OrganizationName(required(organization, "GITHUB_ORG (ou --org)")),
                    parseTopics(required(topics, "TOPICS (ou --topics)")),
                    VariableName.fromNullable(environment.apply("VAR_NAME"))
            );
            var connection = new GitHubConnectionSettings(
                    new GitHubToken(token),
                    GitHubApiUrl.fromNullable(environment.apply("GITHUB_API_URL")),
                    ProxyUrl.fromNullable(environment.apply("PROXY_URL")),
                    NoProxyHosts.parse(environment.apply("NO_PROXY")),
                    CertificatePath.fromNullable(environment.apply("GITHUB_API_CERT_PATH"))
            );
            return new ScannerSettings(criteria, connection);
        } catch (ApplicationConfigurationException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new ApplicationConfigurationException(
                    "Configuração inválida: " + exception.getMessage(),
                    exception
            );
        }
    }

    private Map<String, String> parseOverrides(String[] arguments) {
        var overrides = new LinkedHashMap<String, String>();
        var args = arguments == null ? List.<String>of() : Arrays.asList(arguments);

        for (var index = 0; index < args.size(); index++) {
            var argument = args.get(index);
            if (argument.startsWith("--org=")) {
                overrides.put("org", argument.substring("--org=".length()));
            } else if (argument.startsWith("--topics=")) {
                overrides.put("topics", argument.substring("--topics=".length()));
            } else if ("--org".equals(argument) || "--topics".equals(argument)) {
                if (index + 1 >= args.size()) {
                    throw new ApplicationConfigurationException(
                            "A opção " + argument + " exige um valor.");
                }
                overrides.put(argument.substring(2), args.get(++index));
            }
        }
        return overrides;
    }

    private List<RepositoryTopic> parseTopics(String rawTopics) {
        var topics = new ArrayList<RepositoryTopic>();
        for (var rawTopic : rawTopics.split(",")) {
            if (!rawTopic.isBlank()) {
                topics.add(new RepositoryTopic(rawTopic));
            }
        }
        if (topics.isEmpty()) {
            throw new ApplicationConfigurationException(
                    "Configuração obrigatória ausente: TOPICS (ou --topics).");
        }
        return List.copyOf(topics);
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ApplicationConfigurationException(
                    "Configuração obrigatória ausente: " + name
                            + ". Preencha o .env (veja .env.example) ou informe a opção de CLI correspondente."
            );
        }
        return value.trim();
    }
}
