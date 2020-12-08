package com.instagramfake.auth;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;

public abstract class AuthenticationProvider {
    private final String username;

    protected AuthenticationProvider(String username) {
        this.username = username;
    }

    public abstract void setAuthentication(CloseableHttpClient client) throws IOException;

    public abstract Optional<BasicCookieStore> loadCookieStore();

    public abstract void save(BasicCookieStore cookieStore);

    public String getUsername() {
        return username;
    }
}
