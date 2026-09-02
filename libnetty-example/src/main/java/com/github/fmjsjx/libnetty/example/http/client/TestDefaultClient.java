package com.github.fmjsjx.libnetty.example.http.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.*;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.*;
import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;

/**
 * Test class for default client.
 */
public class TestDefaultClient {

    private static final Logger logger = LoggerFactory.getLogger(TestDefaultClient.class);

    private static final AsciiString TEXT_EVENT_STREAM = AsciiString.cached("text/event-stream");

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
            testLineStreamM(client, 18,1000);
            // Synchronous API
            testSynchronousApi(client);
            // Asynchronous API
            testAsynchronousApi(client);
            // Tailing headers API
            testTrailingHeaders(client);
            // Input stream API
            testInputStream(client);
            // Upload API
            testUpload(client);
            // test line stream
            testLineStream(client, 18,null);
            // test line flux
            testLineFlux(client, 15,null);
            // test line stream
            testLineStreamTrailing(client, 12,1);
            // test line flux
            testLineFluxTrailing(client, 8,2);
            try {
                // test line stream
                testLineStream(client, 64, 8);
            } catch (Exception e) {
                logger.info("Should ignore this error in testLineStream", e);
            }
            try {
                // test line flux
                testLineFlux(client, 64, 8);
            } catch (Exception e) {
                logger.info("Should ignore this error in testLineFlux", e);
            }
            // test line flux
            testLineFlux(client, 12,null);
            // test line stream
            testLineStream(client, 12,null);
            // Synchronous API
            testSynchronousApi(client);
            // Asynchronous API
            testAsynchronousApi(client);
        }
    }

    static void testSynchronousApi(HttpClient client) throws IOException, InterruptedException, TimeoutException {
        // GET https://localhost:8443/api/test
        Response<String> response1 = client.request(URI.create("https://localhost:8443/api/test")).get()
                .send(HttpContentHandlers.ofString());
        if (response1.statusCode() == 200) {
            String body = response1.content();
            logger.info("response for sync test: {}", body);
        }
        // POST
        String postBody = "p1=abc&p2=12345&a=1&a=2&a=3";
        Response<String> response2 = client.request(URI.create("https://localhost:8443/api/jsons/form"))
                .post(HttpContentHolders.ofUtf8(postBody)).send(HttpContentHandlers.ofString());
        if (response2.statusCode() == 200) {
            String body = response2.content();
            logger.info("response for sync json form: {}", body);
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
                logger.info("response for async test: {}", body);
            }
        }).whenComplete((v, e) -> cd.countDown());
        // POST
        String postBody = "p1=abc&p2=12345&a=1&a=2&a=3";
        CompletableFuture<Response<String>> future2 = client.request(URI.create("https://localhost:8443/api/jsons/form"))
                .post(HttpContentHolders.ofUtf8(postBody)).sendAsync(HttpContentHandlers.ofString());
        future2.thenAccept(response -> {
            if (response.statusCode() == 200) {
                String body = response.content();
                logger.info("response for async json form: {}", body);
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
            logger.info("response 1: {}", resp);

            body = MultipartBody.builder().addFileUpload("file", "test-pic-2.jpeg", file, "image/jpeg").build();
            resp = client.request(URI.create("https://localhost:8443/api/upload"))
                    .post(body).send(HttpContentHandlers.ofString());
            logger.info("response 2: {}", resp);
        } finally {
            // release ByteBuf finally
            content.release();
        }
    }

    static void testLineStream(HttpClient client, int len, Integer errIndex) throws IOException, InterruptedException, TimeoutException {
        URI uri;
        if (errIndex != null) {
            uri = URI.create("https://localhost:8443/api/test/sse-event-stream?len=" + len + "&err=" + errIndex);
        } else {
            uri = URI.create("https://localhost:8443/api/test/sse-event-stream?len=" + len);
        }
        var resp = client.request(uri).setHeader(ACCEPT, TEXT_EVENT_STREAM).get().send(HttpContentHandlers.ofLines());
        try (var lineStream = resp.content()) {
            lineStream.forEach(System.out::print);
        } catch (Exception e) {
            logger.error("Error occurs when processing lines", e);
            throw e;
        }
    }

    static void testLineStreamM(HttpClient client, int len, int multiple) {
        URI uri = URI.create("https://localhost:8443/api/kotlin/sse-event-stream?len=" + len + "&multiple=" + multiple);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var req = client.request(uri).setHeader(ACCEPT, TEXT_EVENT_STREAM).get();
            req.sendAsync(HttpContentHandlers.ofLines(), executor).thenAccept(resp -> {
                try (var lineStream = resp.content()) {
                    lineStream.forEach(System.out::print);
                } catch (Exception e) {
                    logger.error("Error occurs when processing lines M", e);
                    throw e;
                }
            }).join();
        }
    }

    static void testLineFlux(HttpClient client, int len, Integer errIndex) throws ExecutionException, InterruptedException {
        URI uri;
        if (errIndex != null) {
            uri = URI.create("https://localhost:8443/api/test/sse-event-stream?len=" + len + "&err=" + errIndex);
        } else {
            uri = URI.create("https://localhost:8443/api/test/sse-event-stream?len=" + len);
        }
        var resp = client.request(uri).setHeader(ACCEPT, TEXT_EVENT_STREAM).get().sendAsync(HttpContentHandlers.ofLinesFlux()).get();
        resp.content()
                .doOnNext(System.out::print)
                .doOnError(e -> logger.error("Error occurs when processing lines in flux", e))
                .doOnComplete(() -> logger.info("Completed line flux")).blockLast();
    }
    
    static void testTrailingHeaders(HttpClient client) throws IOException, InterruptedException, TimeoutException {
        var resp = client.request(URI.create("https://localhost:8443/api/test/trailing")).get().send(HttpContentHandlers.ofString());
        if (resp.statusCode() == 200) {
            String body = resp.content();
            logger.info("response for tailing headers: {}", body);
            logger.info("headers for tailing headers: {}", resp.headers());
            logger.info("trailing headers: {}", resp.trailingHeaders());
        }
    }

    static void testInputStream(HttpClient client) throws IOException, InterruptedException, TimeoutException {
        Response<InputStream> response1 = client.request(URI.create("https://localhost:8443/api/test")).get()
                .send(HttpContentHandlers.ofInputStream());
        if (response1.statusCode() == 200) {
            try (var input = response1.content()) {
                var body = input.readAllBytes();
                logger.info("response for sync input stream test: {}", new String(body, StandardCharsets.UTF_8));
            }
        }
        // POST
        var postBody = "p1=abc&p2=12345&a=1&a=2&a=3";
        Response<InputStream> response2 = client.request(URI.create("https://localhost:8443/api/jsons/form"))
                .post(HttpContentHolders.ofUtf8(postBody)).send(HttpContentHandlers.ofInputStream());
        if (response2.statusCode() == 200) {
            try (var input = response2.content()) {
                var body = input.readAllBytes();
                logger.info("response for sync input stream json form: {}", new String(body, StandardCharsets.UTF_8));
            }

        }
    }

    static void testLineStreamTrailing(HttpClient client, int len, int trailingMode) throws IOException, InterruptedException, TimeoutException {
        URI uri= URI.create("https://localhost:8443/api/test/sse-event-stream?len=" + len + "&trailing=" + trailingMode);
        var resp = client.request(uri).setHeader(ACCEPT, TEXT_EVENT_STREAM).get().send(HttpContentHandlers.ofLines());
        try (var lineStream = resp.content()) {
            lineStream.forEach(System.out::print);
        } catch (Exception e) {
            logger.error("Error occurs when processing lines trailing", e);
            throw e;
        }
        logger.info("Completed line stream with trailing headers: {}", resp.trailingHeaders());
    }

    static void testLineFluxTrailing(HttpClient client, int len, int trailingMode) throws ExecutionException, InterruptedException {
        URI uri = URI.create("https://localhost:8443/api/test/sse-event-stream?len=" + len + "&trailing=" + trailingMode);
        var resp = client.request(uri).setHeader(ACCEPT, TEXT_EVENT_STREAM).get().sendAsync(HttpContentHandlers.ofLinesFlux()).get();
        resp.content()
                .doOnNext(System.out::print)
                .doOnError(e -> logger.error("Error occurs when processing lines in flux", e))
                .doOnComplete(() -> logger.info("Completed line flux with trailing headers: {}", resp.trailingHeaders()))
                .blockLast();
    }

    private TestDefaultClient() {
    }

}
