package com.github.fmjsjx.libnetty.example.http.client;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.*;
import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;
import io.netty.buffer.ByteBufAllocator;

/**
 * Test class for default client.
 */
public class TestDefaultClient {

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        try (HttpClient client = DefaultHttpClient.builder()
                .sslContextProvider(SslContextProviders.insecureForClient())
                .enableCompression().maxCachedSizeEachDomain(32).build()) {
            // Synchronous API
            testSynchronousApi(client);
            // Asynchronous API
            testAsynchronousApi(client);
            // Upload API
            testUpload(client);
        }
    }

    static void testSynchronousApi(HttpClient client) throws IOException, InterruptedException, TimeoutException {
        // GET https://localhost:8443/api/test
        Response<String> response1 = client.request(URI.create("https://localhost:8443/api/test")).get()
                .send(HttpContentHandlers.ofString());
        if (response1.statusCode() == 200) {
            String body = response1.content();
            System.out.println(body);
        }
        // POST
        String postBody = "p1=abc&p2=12345&a=1&a=2&a=3";
        Response<String> response2 = client.request(URI.create("https://localhost:8443/api/jsons/form"))
                .post(HttpContentHolders.ofUtf8(postBody)).send(HttpContentHandlers.ofString());
        if (response2.statusCode() == 200) {
            String body = response2.content();
            System.out.println(body);
        }
    }

    static void testAsynchronousApi(HttpClient client) throws InterruptedException {
        CountDownLatch cd = new CountDownLatch(2);
        // GET
        CompletableFuture<Response<String>> future1 = client.request(URI.create("https://localhost:8443/api/test")).get()
                .sendAsync(HttpContentHandlers.ofString());
        future1.thenAccept(response -> {
            if (response.statusCode() == 200) {
                String body = response.content();
                System.out.println(body);
            }
        }).whenComplete((v, e) -> cd.countDown());
        // POST
        String postBody = "p1=abc&p2=12345&a=1&a=2&a=3";
        CompletableFuture<Response<String>> future2 = client.request(URI.create("https://localhost:8443/api/jsons/form"))
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

    static void testUpload(HttpClient client) throws InterruptedException, IOException, TimeoutException {
        //noinspection DataFlowIssue
        var file = new File(TestDefaultClient.class.getResource("/test-pic.jpeg").getFile());
        var content = ByteBufAllocator.DEFAULT.buffer();
        try (var ch = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            content.writeBytes(ch, 0, (int) ch.size());
        }
        try {
            var body = MultipartBody.builder().addFileUpload("file", "test-pic-1.jpeg", "image/jpeg", content::retainedDuplicate).build();
            var resp = client.request(URI.create("https://localhost:8443/api/upload"))
                    .post(body).send(HttpContentHandlers.ofString());
            System.out.println(resp);

            body = MultipartBody.builder().addFileUpload("file", "test-pic-2.jpeg", file, "image/jpeg").build();
            resp = client.request(URI.create("https://localhost:8443/api/upload"))
                    .post(body).send(HttpContentHandlers.ofString());
            System.out.println(resp);
        } finally {
            // release ByteBuf finally
            content.release();
        }
    }

    private TestDefaultClient() {
    }

}
