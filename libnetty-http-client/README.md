# LibNetty HTTP Client Project

Provides a simplified HTTP client, supports both synchronous and asynchronous(based on JDK8+ CompletableFuture) APIs.

## Features

- Basic calls on HTTP/1.1
- Support HTTPS(OpenSSL, JdkSSL)
- Support Proxys(HTTP, Socks5, Socks4)
- Simple support for content compression(gzip, deflate, Brotli)
- Non-blocking, asynchronous, API

## Quick Start

Synchronous API:

```java
import com.github.fmjsjx.libnetty.http.client.HttpClient;

try (HttpClient client = HttpClient.defaultBuilder().compression(true).build()) {
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
```

Asynchronous API:

```java
import com.github.fmjsjx.libnetty.http.client.HttpClient;

try (HttpClient client = HttpClient.defaultBuilder().compression(true).build()) {
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
            .post(HttpContentHolders.ofUtf8(postBody)).send(HttpContentHandlers.ofString());
    future2.thenAccept(response -> {
        if (response.statusCode() == 200) {
            String body = response.content();
            System.out.println(body);
        }
    }).whenComplete((v, e) -> cd.countDown());
    // wait requests completed
    cd.await();
}
```