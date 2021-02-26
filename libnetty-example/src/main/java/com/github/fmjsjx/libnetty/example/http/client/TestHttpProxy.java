package com.github.fmjsjx.libnetty.example.http.client;

import java.net.URI;

import com.github.fmjsjx.libnetty.http.client.DefaultHttpClient;
import com.github.fmjsjx.libnetty.http.client.HttpClient;
import com.github.fmjsjx.libnetty.http.client.HttpContentHandlers;
import com.github.fmjsjx.libnetty.http.client.ProxyHandlerFactories;
import com.github.fmjsjx.libnetty.http.client.SimpleHttpClient;
import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;


public class TestHttpProxy {

    public static void main(String[] args) throws Exception {
        try (HttpClient client = DefaultHttpClient.builder()
                .proxyHandlerFactory(ProxyHandlerFactories.forHttp("127.0.0.1", 10809)).enableBrotli().build()) {
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36";
            Response<String> response = client.request(URI.create("https://www.facebook.com/"))
                    .header("accept", "text/html").header("user-agent", userAgent).get()
                    .send(HttpContentHandlers.ofString());
            System.out.println("-- response --");
            System.out.println(response.status());
            System.out.println(response.headers());
        }

        try (HttpClient client = SimpleHttpClient.builder()
                .proxyHandlerFactory(ProxyHandlerFactories.forHttp("127.0.0.1", 10809)).enableBrotli().build()) {
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36";
            Response<String> response = client.request(URI.create("https://www.facebook.com/"))
                    .header("accept", "text/html").header("user-agent", userAgent).get()
                    .send(HttpContentHandlers.ofString());
            System.out.println("-- response --");
            System.out.println(response.status());
            System.out.println(response.headers());
        }
    }

}
