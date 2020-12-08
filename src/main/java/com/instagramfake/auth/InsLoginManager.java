package com.instagramfake.auth;

import com.instagramfake.http.HttpUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InsLoginManager {
    private static final String LOGIN_URL = "https://www.instagram.com/accounts/login/";
    private final CloseableHttpClient client;
    private final AuthenticationProvider authenticationProvider;
    private final Lock lock = new ReentrantLock();

    public InsLoginManager(CloseableHttpClient client, AuthenticationProvider authenticationProvider) {
        this.client = client;
        this.authenticationProvider = authenticationProvider;
    }

    public boolean checkAuthentication(String content, HttpClientContext context) throws IOException {
        URI last = getLastRedirectURI(context);
        if (last != null && last.toString().startsWith(LOGIN_URL)) {
            lock.lock();
            try {
                //check authentication by url
                if (isAuthenticated()) {
                    return true;
                }
                authenticate();
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    public void authenticate() throws IOException {
        this.authenticationProvider.setAuthentication(client);
    }

    private boolean isAuthenticated() throws IOException {
        HttpClientContext context = HttpClientContext.create();
        HttpUtils.connect(client, new HttpGet(LOGIN_URL), context);
        URI uri = getLastRedirectURI(context);
        return uri != null && uri.toString().equals("https://www.instagram.com/");
    }

    private URI getLastRedirectURI(HttpClientContext context) {
        if (context == null) {
            return null;
        }
        List<URI> locations = context.getRedirectLocations();
        if (locations != null) {
            return locations.get(locations.size() - 1);
        }
        return null;
    }
}
