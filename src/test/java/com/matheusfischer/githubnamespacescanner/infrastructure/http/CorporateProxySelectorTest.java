package com.matheusfischer.githubnamespacescanner.infrastructure.http;

import com.matheusfischer.githubnamespacescanner.domain.model.NoProxyHosts;
import com.matheusfischer.githubnamespacescanner.domain.model.ProxyUrl;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CorporateProxySelectorTest {

    @Test
    void shouldUseConfiguredProxy() {
        var selector = new CorporateProxySelector(
                ProxyUrl.fromNullable("http://proxy.example.com:8080"),
                NoProxyHosts.parse(null)
        );

        var proxy = selector.select(URI.create("https://api.github.com")).getFirst();

        assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
        assertThat((InetSocketAddress) proxy.address())
                .extracting(InetSocketAddress::getHostString, InetSocketAddress::getPort)
                .containsExactly("proxy.example.com", 8080);
    }

    @Test
    void shouldUseDirectConnectionWhenProxyIsEmptyOrHostMatchesNoProxy() {
        var withoutProxy = new CorporateProxySelector(Optional.empty(), NoProxyHosts.parse(null));
        var bypass = new CorporateProxySelector(
                ProxyUrl.fromNullable("http://proxy.example.com:8080"),
                NoProxyHosts.parse("localhost,*.internal.example.com")
        );

        assertThat(withoutProxy.select(URI.create("https://api.github.com")))
                .containsExactly(Proxy.NO_PROXY);
        assertThat(bypass.select(URI.create("http://localhost:8080")))
                .containsExactly(Proxy.NO_PROXY);
        assertThat(bypass.select(URI.create("https://api.internal.example.com")))
                .containsExactly(Proxy.NO_PROXY);
    }
}
