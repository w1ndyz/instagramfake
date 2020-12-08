package com.instagramfake.config;

public class DefaultProxyConfig implements ProxyConfig {
    private final String addr;
    private final Integer port;

    public DefaultProxyConfig(String addr, Integer port) {
        this.addr = addr;
        this.port = port;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getAddr() {
        return addr;
    }

    @Override
    public boolean isEnable() {
        return addr != null && port != null && !addr.isEmpty();
    }
}
