package com.instagramfake.auth;

import com.instagramfake.encrypt.SealedBoxUtility;
import com.instagramfake.http.HttpUtils;
import com.instagramfake.http.InvalidStateCodeException;
import com.instagramfake.util.JsonExecutor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class InsLoginProvider extends AuthenticationProvider{

    private final String password;
    private final CookieStorePersistence persistence;

    public InsLoginProvider(String username, String password, CookieStorePersistence persistence) {
        super(username);
        this.password = password;
        this.persistence = persistence == null ? new CookieStorePersistence() {
            @Override
            public void store(String username, BasicCookieStore cookieStore) {

            }

            @Override
            public Optional<BasicCookieStore> loadCookieStore(String username) {
                return Optional.empty();
            }
        } : persistence;
    }

    @Override
    public void setAuthentication(CloseableHttpClient client) throws IOException {
        this.setAuthentication(HttpClientContext.create(), client);
    }

    @Override
    public Optional<BasicCookieStore> loadCookieStore() {
        return persistence.loadCookieStore(getUsername());
    }

    @Override
    public void save(BasicCookieStore cookieStore) {
        persistence.store(getUsername(), cookieStore);
    }

    protected void setAuthentication(HttpClientContext context, CloseableHttpClient client) throws IOException {
        String content = HttpUtils.toString(client, "https://www.instagram.com/data/shared_data/", context);
        JsonExecutor executor = new JsonExecutor(content);
        int key = executor.execute("encryption->key_id").getAsInt();
        int version = executor.execute("encryption->version").getAsInt();
        String publicKey = executor.execute("encryption->public_key").getAsString();
        HttpPost post = new HttpPost("https://www.instagram.com/accounts/login/ajax/");
        String time = String.valueOf(System.currentTimeMillis() / 1000);
        String csrfToken = getCsrfToken(context);
        post.addHeader("referer", "https://www.instagram.com/");
        post.addHeader("X-CSRFToken", csrfToken);
        post.addHeader("x-requested-with", "XMLHttpRequest");
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("username", getUsername()));
        String encrypt;
        try {
            encrypt = encrypt(key, publicKey, time);
        } catch (Exception e) {
            throw new RuntimeException("fail to encrypt:" + e.getMessage(), e);
        }
        pairs.add(new BasicNameValuePair("enc_password", "#PWD_INSTAGRAM_BROWSER:" + version + ":" + time + ":" + encrypt));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);
        post.setEntity(entity);
        String loginResult;
        try {
            loginResult = HttpUtils.toString(client, post, context);
        } catch (InvalidStateCodeException ex) {
            if (ex.getContent().contains("\"two_factor_required\": true")) {
                String identifier = new JsonExecutor(ex.getContent()).execute("two_factor_info->two_factor_identifier").getAsString();
//                twoFactorLogin(context, identifier, client);
                return;
            }
            throw new LoginFailException(ex.getContent());
        }
        if (loginResult.contains("\"authenticated\": true")) {
            return;
        }
        throw new RuntimeException("fail to login , login result : " + loginResult);
    }

    private String getCsrfToken(HttpClientContext context) {
        return context.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("csrftoken")).map(Cookie::getValue).findAny().orElseThrow(() -> new RuntimeException("fail to get csrf token"));
    }

    private String encrypt(int key, String pkey, String time) throws Exception {
        int overheadLength = 48;
        byte[] pkeyArray = new byte[pkey.length() / 2];
        for (int i = 0; i < pkeyArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(pkey.substring(index, index + 2), 16);
            pkeyArray[i] = (byte) j;
        }

        byte[] y = new byte[password.length() + 36 + 16 + overheadLength];

        int f = 0;
        y[f] = 1;
        y[f += 1] = (byte) key;
        f += 1;

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);

        // Generate Key
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] IV = new byte[12];

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        cipher.updateAAD(time.getBytes());

        byte[] sealed = SealedBoxUtility.crypto_box_seal(secretKey.getEncoded(), pkeyArray);
        byte[] cipherText = cipher.doFinal(password.getBytes());
        y[f] = (byte) (255 & sealed.length);
        y[f + 1] = (byte) (sealed.length >> 8 & 255);
        f += 2;
        System.arraycopy(sealed, 0, y, f, f + sealed.length - f);
        f += 32;
        f += overheadLength;

        byte[] c = Arrays.copyOfRange(cipherText, cipherText.length - 16, cipherText.length);
        byte[] h = Arrays.copyOfRange(cipherText, 0, cipherText.length - 16);

        System.arraycopy(c, 0, y, f, f + c.length - f);
        f += 16;
        System.arraycopy(h, 0, y, f, f + h.length - f);
        return Base64.getEncoder().encodeToString(y);
    }
}
