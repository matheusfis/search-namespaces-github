package com.matheusfischer.githubnamespacescanner.infrastructure.http;

import com.matheusfischer.githubnamespacescanner.domain.exception.ApplicationConfigurationException;
import com.matheusfischer.githubnamespacescanner.domain.model.CertificatePath;
import com.matheusfischer.githubnamespacescanner.domain.model.GitHubConnectionSettings;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubHttpClientFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    public RestTemplate create(GitHubConnectionSettings settings) {
        var clientBuilder = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(new CorporateProxySelector(settings.proxyUrl(), settings.noProxyHosts()));

        settings.certificatePath()
                .map(this::sslContext)
                .ifPresent(clientBuilder::sslContext);

        var requestFactory = new JdkClientHttpRequestFactory(clientBuilder.build());
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(requestFactory);
    }

    private SSLContext sslContext(CertificatePath certificatePath) {
        try {
            var defaultManager = trustManager(null);
            var customTrustStore = customTrustStore(certificatePath);
            var customManager = trustManager(customTrustStore);
            var context = SSLContext.getInstance("TLS");
            context.init(
                    null,
                    new X509TrustManager[]{new CompositeX509TrustManager(List.of(defaultManager, customManager))},
                    null
            );
            return context;
        } catch (GeneralSecurityException | IOException exception) {
            throw new ApplicationConfigurationException(
                    "Could not load GITHUB_API_CERT_PATH: " + certificatePath.value(),
                    exception
            );
        }
    }

    private KeyStore customTrustStore(CertificatePath certificatePath)
            throws GeneralSecurityException, IOException {
        if (!Files.isRegularFile(certificatePath.value()) || !Files.isReadable(certificatePath.value())) {
            throw new ApplicationConfigurationException(
                    "GITHUB_API_CERT_PATH must point to a readable certificate file: "
                            + certificatePath.value()
            );
        }

        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        var certificateFactory = CertificateFactory.getInstance("X.509");
        try (InputStream input = Files.newInputStream(certificatePath.value())) {
            var certificates = new ArrayList<>(certificateFactory.generateCertificates(input));
            if (certificates.isEmpty()) {
                throw new ApplicationConfigurationException(
                        "GITHUB_API_CERT_PATH does not contain an X.509 certificate: "
                                + certificatePath.value()
                );
            }
            for (var index = 0; index < certificates.size(); index++) {
                keyStore.setCertificateEntry(
                        "custom-ca-" + index,
                        (X509Certificate) certificates.get(index)
                );
            }
        }
        return keyStore;
    }

    private X509TrustManager trustManager(KeyStore keyStore) throws GeneralSecurityException {
        var factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        return List.of(factory.getTrustManagers()).stream()
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst()
                .orElseThrow(() -> new GeneralSecurityException("No X.509 trust manager is available"));
    }
}
