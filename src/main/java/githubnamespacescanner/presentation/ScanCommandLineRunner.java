package githubnamespacescanner.presentation;

import githubnamespacescanner.application.MarkdownReportFormatter;
import githubnamespacescanner.application.ScanRepositoriesUseCase;
import githubnamespacescanner.domain.exception.ApplicationConfigurationException;
import githubnamespacescanner.domain.exception.GitHubApiException;
import githubnamespacescanner.domain.exception.GitHubAuthenticationException;
import githubnamespacescanner.domain.exception.GitHubNotFoundException;
import githubnamespacescanner.domain.model.ScanResult;
import githubnamespacescanner.infrastructure.configuration.DotenvScannerSettingsLoader;
import githubnamespacescanner.infrastructure.github.GitHubRestClientAdapter;
import githubnamespacescanner.infrastructure.http.GitHubHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ScanCommandLineRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanCommandLineRunner.class);
    private static final Path REPORT_FILE = Path.of("namespaces-report.md");

    private final DotenvScannerSettingsLoader settingsLoader;
    private final GitHubHttpClientFactory httpClientFactory;
    private int exitCode;

    public ScanCommandLineRunner(
            DotenvScannerSettingsLoader settingsLoader,
            GitHubHttpClientFactory httpClientFactory
    ) {
        this.settingsLoader = settingsLoader;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            var settings = settingsLoader.load(arguments.getSourceArgs());
            LOGGER.info(
                    "Varredura -> org=\"{}\" | topics(AND)={} | var=\"{}\" | api={}",
                    settings.criteria().organization().value(),
                    settings.criteria().topics().stream().map(topic -> topic.value()).toList(),
                    settings.criteria().variableName().value(),
                    settings.connection().apiUrl().value()
            );

            var restTemplate = httpClientFactory.create(settings.connection());
            var github = new GitHubRestClientAdapter(restTemplate, settings.connection());
            var result = new ScanRepositoriesUseCase(github).execute(settings.criteria());
            var report = new MarkdownReportFormatter().format(result);

            System.out.println(report);
            Files.writeString(REPORT_FILE, report, StandardCharsets.UTF_8);
            logSummary(result);
        } catch (ApplicationConfigurationException exception) {
            exitCode = 2;
            LOGGER.error("[Configuração] {}", exception.getMessage());
        } catch (GitHubAuthenticationException exception) {
            exitCode = 3;
            logGitHubError("[Auth]", exception);
        } catch (GitHubNotFoundException exception) {
            exitCode = 4;
            logGitHubError("[GitHub]", exception);
        } catch (GitHubApiException exception) {
            exitCode = exception.statusCode() == 403 ? 5 : 1;
            logGitHubError("[GitHub]", exception);
        } catch (Exception exception) {
            exitCode = 1;
            LOGGER.error("[Erro inesperado] {}", exception.getMessage(), exception);
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void logSummary(ScanResult result) {
        LOGGER.info(
                "Resumo: {} repo(s) ativo(s) na org | {} com todos os topics | "
                        + "{} com {} | {} namespace(s) único(s) | {} sem variável | "
                        + "{} erro(s). Relatório: {}",
                result.totalOrganizationRepositories(),
                result.analyzedRepositories(),
                result.repositoriesWithVariable(),
                result.criteria().variableName().value(),
                result.namespaces().size(),
                result.repositoriesWithoutVariable().size(),
                result.errors().size(),
                REPORT_FILE
        );
    }

    private void logGitHubError(String category, GitHubApiException exception) {
        LOGGER.error("{} {}", category, exception.diagnosticMessage());
    }
}
