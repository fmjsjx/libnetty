package com.github.fmjsjx.libnetty.example.http.client;

import static com.github.fmjsjx.libnetty.example.http.client.TestDefaultClient.*;
import com.github.fmjsjx.libnetty.http.client.HttpClient;

public class TestSimpleClient {

    public static void main(String[] args) throws Exception {
        try (HttpClient client = HttpClient.simpleBuilder().compression(true).build()) {
            // class com.github.fmjsjx.libnetty.http.client.SimpleHttpClient
            System.out.println(client.getClass());
            // SimpleHttpClient always creates and closes channel for each request.

            // Synchronous API
            testSynchronousApi(client);
            // Asynchronous API
            testAsynchronousApi(client);
        }
    }
}
