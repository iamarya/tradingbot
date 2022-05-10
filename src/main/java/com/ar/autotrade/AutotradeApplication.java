package com.ar.autotrade;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class AutotradeApplication {

    public static void main(String[] args) {
        init();
        new SpringApplicationBuilder(AutotradeApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public OkHttpClient getHTTPClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .cache(null)
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        String domain = url.url().getHost();
                        cookieStore.put(domain, cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        String domain = url.url().getHost();
                        List<Cookie> cookies = cookieStore.get(domain);
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();
        return client;
    }

    public static void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

}
