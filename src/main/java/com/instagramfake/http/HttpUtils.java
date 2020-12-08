package com.instagramfake.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UncheckedIOException;

public class HttpUtils {
    private static final ResponseHandler<Void> emptyHandler = httpResponse -> null;
    private static final ResponseHandler<String> stringHandler = response -> {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new IllegalStateException("no response content");
        }
        String content = EntityUtils.toString(entity);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 300) {
            throw new InvalidStateCodeException(statusCode, "错误的状态码:" + statusCode, content);
        }
        return content;
    };

    public static String toString(CloseableHttpClient client, String url, HttpContext context) {
        return toString(client, new HttpGet(url), context);
    }

    public static void connect(CloseableHttpClient client, HttpRequestBase req, HttpContext context) throws IOException {
        try {
            client.execute(req, emptyHandler, context);
        } finally {
            req.releaseConnection();
        }
    }

    public static String toString(CloseableHttpClient client, HttpRequestBase req, HttpContext context) {
        try {
            return client.execute(req, stringHandler, context);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            req.releaseConnection();
        }
    }
}
