package com.instagramfake;

import com.instagramfake.auth.CookieStorePersistence;
import com.instagramfake.auth.InsLoginProvider;
import com.instagramfake.config.ProxyConfig;
import com.instagramfake.config.DefaultHttpConfig;
import com.instagramfake.config.DefaultProxyConfig;
import com.instagramfake.config.HttpConfig;
import com.instagramfake.http.RoutePlanner;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
public class LoginController {
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36 RuxitSynthetic/1.0 v7628303799 t38550 ath9b965f92 altpub cvcv=2";
    private static final int TIME_OUT = 30 * 1000;

    @RequestMapping("/login")
    public String login(String username, String password) throws IOException {
        final Path dir = Paths.get(System.getProperty("user.home")).resolve("work")
                .resolve("ins");
        CookieStorePersistence persistence = new CookieStorePersistence() {
            @Override
            public void store(String username, BasicCookieStore cookieStore) {
                Path path = dir.resolve(username);
                FileWriter writer;
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (ObjectOutputStream outputStream = new ObjectOutputStream(buffer)) {
                    File file = new File(path.toString());
                    if (!file.exists()) {    //文件不存在则创建文件，先创建目录
                        File dir = new File(file.getParent());
                        dir.mkdirs();
                        file.createNewFile();
                    }
                    System.out.println("文件地址为:" + path.toString());
                    writer = new FileWriter(path.toString());
                    for(Cookie cookie:cookieStore.getCookies()){
                        writer.write(cookie.getName() + ":" + cookie.getValue() + "\n");
                    }
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public Optional<BasicCookieStore> loadCookieStore(String username) {
                Path file = dir.resolve(username);
                if (Files.exists(file)) {
                    try {
                        byte[] raw = Files.readAllBytes(file);
                        final ByteArrayInputStream buffer = new ByteArrayInputStream(raw);
                        try (ObjectInputStream inputStream = new ObjectInputStream(buffer)) {
                            return Optional.of((BasicCookieStore) inputStream.readObject());
                        } catch (ClassNotFoundException ignored) {
                        }
                    } catch (IOException ignored) {

                    }
                }
                return Optional.empty();
            }
        };
        System.out.println("username is:"+username);
        System.out.println("password is:"+password);
        InsLoginProvider insLoginProvider = new InsLoginProvider(username, password, persistence);
        BasicCookieStore cookieStore = insLoginProvider.loadCookieStore().orElse(new BasicCookieStore());
        DefaultHttpConfig defaultHttpConfig = new DefaultHttpConfig();
        defaultHttpConfig.setProxyConfig(new DefaultProxyConfig("127.0.0.1", 58591));
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(TIME_OUT)
                        .setConnectTimeout(TIME_OUT)
                        .setSocketTimeout(TIME_OUT)
                        .setCookieSpec(CookieSpecs.DEFAULT).build()).
                        addInterceptorLast((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                            httpRequest.addHeader("user-agent", USER_AGENT);
                        })
                .setDefaultCookieStore(new CookieStoreDelegate(insLoginProvider,cookieStore))
                .setMaxConnPerRoute(defaultHttpConfig.getMaxConnections())
                .setRoutePlanner(new RoutePlanner(defaultHttpConfig.getProxyConfig())).build();
        insLoginProvider.setAuthentication(client);
        return "cookie文件已生成!";
    }

    private final class CookieStoreDelegate implements CookieStore {
        private final InsLoginProvider insLoginProvider;
        private final BasicCookieStore basicCookieStore;

        private CookieStoreDelegate(InsLoginProvider insLoginProvider, BasicCookieStore basicCookieStore) {
            this.insLoginProvider = insLoginProvider;
            this.basicCookieStore = basicCookieStore;
        }

        @Override
        public void addCookie(Cookie cookie) {
            basicCookieStore.addCookie(cookie);
            insLoginProvider.save(basicCookieStore);
        }

        @Override
        public List<Cookie> getCookies() {
            return basicCookieStore.getCookies();
        }

        @Override
        public boolean clearExpired(Date date) {
            if (basicCookieStore.clearExpired(date)) {
                insLoginProvider.save(basicCookieStore);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            basicCookieStore.clear();
            insLoginProvider.save(basicCookieStore);
        }
    }
}
