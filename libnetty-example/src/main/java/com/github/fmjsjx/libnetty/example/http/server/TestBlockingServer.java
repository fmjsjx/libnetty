package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.component.DefaultWorkerPool;
import com.github.fmjsjx.libnetty.http.server.component.JsonLibrary;
import com.github.fmjsjx.libnetty.http.server.component.SimpleExceptionHandler;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.LogFormat;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.Slf4jLoggerWrapper;
import com.github.fmjsjx.libnetty.http.server.middleware.AuthBasic;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.middleware.ServeStatic;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Test blocking server
 */
@Slf4j
public class TestBlockingServer {

    private static final Map<String, String> passwds() {
        return Collections.singletonMap("test", "123456");
    }

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        BlockingTestController controller = new BlockingTestController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServer server = new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(ChannelSslInitializer.of(SslContextProviders.selfSignedForServer())) // SSL
                .neverTimeout() // never timeout
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                .supportJson() // Support JSON using embedded json library
                .component(new DefaultWorkerPool(1)) // support blocking APIs
                .component(new TestExceptionHandler()) // support test error handler
                .soBackLog(1024).tcpNoDelay() // channel options
                .applyCompressionOptions( // compression support
                        HttpContentCompressorProvider.defaultOptions());
        server.defaultHandlerProvider() // use default server handler (DefaultHttpServerHandlerProvider)
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2)) // access logger
                .addLast("/static/auth", new AuthBasic(passwds(), "test")) // HTTP Basic Authentication
                .addLast(new ServeStatic("/static/", "libnetty-example/src/main/resources/static/")) // static resources
                .addLast(new Router().register(controller).init()) // router
        ;
        try {
            server.startup();
            log.info("Server {} started.", server);
            //noinspection ResultOfMethodCallIgnored
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

class TestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TestException() {
        super();
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestException(String message) {
        super(message);
    }

    public TestException(Throwable cause) {
        super(cause);
    }

}

class TestExceptionHandler extends SimpleExceptionHandler<TestException> {

    @Override
    protected CompletionStage<HttpResult> handle0(HttpRequestContext ctx, TestException cause) {
        var obj = Map.of("error", cause.toString());
        //noinspection OptionalGetWithoutIsPresent
        var content = ctx.component(JsonLibrary.class).get().write(ctx.alloc(), obj);
        return ctx.simpleRespond(HttpResponseStatus.OK, content, HttpHeaderValues.APPLICATION_JSON);
    }

}