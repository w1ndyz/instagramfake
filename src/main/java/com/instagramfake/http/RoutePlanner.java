package com.instagramfake.http;

import com.instagramfake.config.ProxyConfig;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;

public class RoutePlanner implements HttpRoutePlanner {
    private final ProxyConfig config;

    private final DefaultRoutePlanner drp = new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE);

    public RoutePlanner(ProxyConfig config) {
        this.config = config;
    }

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        if (config != null && config.isEnable()) {
            return new DefaultProxyRoutePlanner(new HttpHost(config.getAddr(),
                    config.getPort()))
                    .determineRoute(target, request, context);
        }
        return drp.determineRoute(target, request, context);
    }
}
