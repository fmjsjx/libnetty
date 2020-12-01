# LibNetty HTTP Server Project

Provides a simplified HTTP server framework.

## Features

- Support HTTP/1.x
- Support HTTPS(OpenSSL, JdkSSL)
- Simple support for content compression(gzip, deflate)
- Non-blocking, asynchronous, API
- Optional support for JSON
- Blocking mode API for JSON responses

## Quick Start

HelloController.java
```java
@HttpPath("/api")
public class HelloController {
    @HttpGet("/jsons")
    @JsonBody
    public CompletableFuture<?> getJsons(QueryStringDecoder query, EventLoop eventLoop) {
        // GET /jsons
        System.out.println("-- jsons --");
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        query.parameters().forEach((key, values) -> {
            if (values.size() == 1) {
                node.put(key, values.get(0));
            } else {
                node.putPOJO(key, values);
            }
        });
        return CompletableFuture.supplyAsync(() -> {
            if (node.isEmpty()) {
                throw new ManualHttpFailureException(BAD_REQUEST, "{\"code\":1,\"message\":\"Missing Query String\"}",
                        HttpHeaderValues.APPLICATION_JSON, "Missing Query String");
            } else {
                return node;
            }
        }, eventLoop);
    }
}
```

HelloServer.java
```java
public class HelloServer {
    public static void main(String[] args) {
        HelloController controller = new HelloController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServer server = new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(SslContextProviders.selfSignedForServer()) // SSL
                .neverTimeout() // never timeout
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                .soBackLog(1024).tcpNoDelay() // channel options
                .applyCompressionSettings( // compression support
                        HttpContentCompressorFactory.defaultSettings()) // default settings
        ;
        server.defaultHandlerProvider() // use default server handler (DefaultHttpServerHandlerProvider)
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2)) // access logger
                .addLast(new SupportJson()) // JSON support
                .addLast("/static/auth", new AuthBasic(passwds(), "test")) // HTTP Basic Authentication
                .addLast(new ServeStatic("/static/", "src/main/resources/static/")) // static resources
                .addLast(new Router().register(controller).init()) // router
        ;
        try {
            server.startup();
            log.info("Server {} started.", server);
            System.in.read();
        } catch (Exception e) {
            log.error("Unexpected error occurs when startup {}", server, e);
        } finally {
            if (server.isRunning()) {
                server.shutdown();
                log.info("Server {} stopped.", server);
            }
        }
    }
}
```