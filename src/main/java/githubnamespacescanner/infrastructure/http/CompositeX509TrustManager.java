package githubnamespacescanner.infrastructure.http;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

final class CompositeX509TrustManager implements X509TrustManager {

    private final List<X509TrustManager> delegates;

    CompositeX509TrustManager(List<X509TrustManager> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        check(chain, authType, true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        check(chain, authType, false);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegates.stream()
                .flatMap(delegate -> List.of(delegate.getAcceptedIssuers()).stream())
                .toArray(X509Certificate[]::new);
    }

    private void check(X509Certificate[] chain, String authType, boolean client) throws CertificateException {
        CertificateException lastFailure = null;
        for (var delegate : delegates) {
            try {
                if (client) {
                    delegate.checkClientTrusted(chain, authType);
                } else {
                    delegate.checkServerTrusted(chain, authType);
                }
                return;
            } catch (CertificateException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure == null
                ? new CertificateException("No X.509 trust manager is configured")
                : lastFailure;
    }
}
