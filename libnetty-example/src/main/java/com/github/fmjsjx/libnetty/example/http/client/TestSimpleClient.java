package com.github.fmjsjx.libnetty.example.http.client;

import static com.github.fmjsjx.libnetty.example.http.client.TestDefaultClient.*;
import com.github.fmjsjx.libnetty.http.client.HttpClient;
import com.github.fmjsjx.libnetty.http.client.SimpleHttpClient;

/**
 * Test class for simple client.
 */
public class TestSimpleClient {

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        try (HttpClient client = SimpleHttpClient.builder().enableCompression().build()) {
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
