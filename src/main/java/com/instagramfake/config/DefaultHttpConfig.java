package com.instagramfake.config;

public class DefaultHttpConfig implements HttpConfig {
    private int maxConnections = 20;
    private int connectTimeout = 30 * 1000;
    private int socketTimeout = 5 * 1000;
    private int connectionRequestTimeout = 30 * 1000;
    private ProxyConfig proxyConfig;

    @Override
    public int getMaxConnections() {
        return maxConnections;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public int getSocketTimeout() {
        return socketTimeout;
    }

    @Override
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    @Override
    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
}
