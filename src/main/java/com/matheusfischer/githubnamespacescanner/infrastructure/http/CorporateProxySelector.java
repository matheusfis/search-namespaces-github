package com.matheusfischer.githubnamespacescanner.infrastructure.http;

import com.matheusfischer.githubnamespacescanner.domain.model.NoProxyHosts;
import com.matheusfischer.githubnamespacescanner.domain.model.ProxyUrl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CorporateProxySelector extends ProxySelector {

    private final Optional<ProxyUrl> proxyUrl;
    private final NoProxyHosts noProxyHosts;

    public CorporateProxySelector(Optional<ProxyUrl> proxyUrl, NoProxyHosts noProxyHosts) {
        this.proxyUrl = Objects.requireNonNull(proxyUrl, "Proxy URL optional must not be null");
        this.noProxyHosts = Objects.requireNonNull(noProxyHosts, "NO_PROXY hosts must not be null");
    }

    @Override
    public List<Proxy> select(URI uri) {
        Objects.requireNonNull(uri, "URI must not be null");
        if (proxyUrl.isEmpty() || noProxyHosts.matches(uri.getHost())) {
            return List.of(Proxy.NO_PROXY);
        }
        var proxy = proxyUrl.orElseThrow();
        return List.of(new Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved(proxy.host(), proxy.port())
        ));
    }

    @Override
    public void connectFailed(URI uri, SocketAddress address, IOException exception) {
        // HttpClient reports the connection failure to the caller; no mutable proxy state is kept.
    }
}
