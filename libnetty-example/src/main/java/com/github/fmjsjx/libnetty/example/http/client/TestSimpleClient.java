package com.github.fmjsjx.libnetty.example.http.client;

import static com.github.fmjsjx.libnetty.example.http.client.TestDefaultClient.*;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.HttpClient;
import com.github.fmjsjx.libnetty.http.client.SimpleHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for simple client.
 */
public class TestSimpleClient {

    private static final Logger logger = LoggerFactory.getLogger(TestSimpleClient.class);

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        try (HttpClient client = SimpleHttpClient.builder().sslContextProvider(SslContextProviders.insecureForClient())
                .enableCompression().build()) {
            // class com.github.fmjsjx.libnetty.http.client.SimpleHttpClient
            logger.info("client class: {}", client.getClass());
            // SimpleHttpClient always creates and closes channel for each request.

            // Synchronous API
            testSynchronousApi(client);
            // Asynchronous API
            testAsynchronousApi(client);
            // test upload
            testUpload(client);
            // test line stream
            testLineStream(client, 10,null);
            // test line stream
            testLineFlux(client, 10,null);
            // test line stream
            testLineStream(client, 10,null);
            // test line stream
            testLineFlux(client, 10,null);
            try {
                // test line stream
                testLineStream(client, 64, 6);
            } catch (Exception e) {
                logger.info("Should ignore this error in testLineStream", e);
            }
            try {
                // test line stream
                testLineFlux(client, 64, 6);
            } catch (Exception e) {
                logger.info("Should ignore this error in testLineFlux", e);
            }
            // test line stream
            testLineStream(client, 10,null);
            // test line stream
            testLineFlux(client, 10,null);
        }
    }

    private TestSimpleClient() {
    }

}
