package com.instagramfake.config;

public interface HttpConfig {
    default int getMaxConnections() {
        return 20;
    }

    default int getConnectTimeout() {
        return 30 * 1000;
    }

    default int getSocketTimeout() {
        return 5 * 1000;
    }

    default int getConnectionRequestTimeout() {
        return 30 * 1000;
    }

    default ProxyConfig getProxyConfig() {
        return null;
    }
}
