package com.github.fmjsjx.libnetty.example.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import com.github.fmjsjx.libnetty.http.client.HttpClient;
import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;
import com.github.fmjsjx.libnetty.http.client.HttpContentHandlers;
import com.github.fmjsjx.libnetty.http.client.HttpContentHolders;

public class TestDefaultClient {

    public static void main(String[] args) throws Exception {
        try (HttpClient client = HttpClient.defaultBuilder().compression(true).build()) {
            // Synchronous API
            testSynchronousApi(client);
            // Asynchronous API
            testAsynchronousApi(client);
        }
    }

    static void testSynchronousApi(HttpClient client) throws IOException, InterruptedException {
        // GET
        Response<String> response1 = client.request(URI.create("http://127.0.0.1:8080/foo")).get()
                .send(HttpContentHandlers.ofString());
        if (response1.statusCode() == 200) {
            String body = response1.content();
            System.out.println(body);
        }
        // POST
        String postBody = "p1=abc&p2=12345";
        Response<String> response2 = client.request(URI.create("http://127.0.0.1:8080/foo/bar"))
                .post(HttpContentHolders.ofUtf8(postBody)).send(HttpContentHandlers.ofString());
        if (response2.statusCode() == 200) {
            String body = response2.content();
            System.out.println(body);
        }
    }

    static void testAsynchronousApi(HttpClient client) throws IOException, InterruptedException {
        CountDownLatch cd = new CountDownLatch(2);
        // GET
        CompletableFuture<Response<String>> future1 = client.request(URI.create("http://127.0.0.1:8080/foo")).get()
                .sendAsync(HttpContentHandlers.ofString());
        future1.thenAccept(response -> {
            if (response.statusCode() == 200) {
                String body = response.content();
                System.out.println(body);
            }
        }).whenComplete((v, e) -> cd.countDown());
        // POST
        String postBody = "p1=abc&p2=12345";
        CompletableFuture<Response<String>> future2 = client.request(URI.create("http://127.0.0.1:8080/foo/bar"))
                .post(HttpContentHolders.ofUtf8(postBody)).sendAsync(HttpContentHandlers.ofString());
        future2.thenAccept(response -> {
            if (response.statusCode() == 200) {
                String body = response.content();
                System.out.println(body);
            }
        }).whenComplete((v, e) -> cd.countDown());
        // wait requests completed
        cd.await();

    }

}
