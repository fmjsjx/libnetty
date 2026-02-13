package com.github.fmjsjx.libnetty.example.http.server;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.component.JsonLibrary;
import com.github.fmjsjx.libnetty.http.server.component.MixedJsonLibrary;
import com.github.fmjsjx.libnetty.http.server.component.WebSocketSupport;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.LogFormat;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.Slf4jLoggerWrapper;
import com.github.fmjsjx.libnetty.http.server.middleware.AuthBasic;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.middleware.ServeStatic;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

import static io.netty.handler.codec.http.HttpMethod.*;


/**
 * Test class for default server.
 */
@Slf4j
public class TestHttp2Server {

    private static final Map<String, String> passwords() {
        return Collections.singletonMap("test", "123456");
    }

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        var controller = new TestController();
        var kotlinController = new KotlinController();
        var corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        var sslCtxProvider = SslContextProviders.selfSignedForH2Server();
        var server = new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(ChannelSslInitializer.of(sslCtxProvider)) // SSL
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                .enableHttp2()
                // support JSON using MixedJsonLibrary
                .component(MixedJsonLibrary.Builder.recommended().emptyWay(JsonLibrary.EmptyWay.EMPTY)
                        .beforeWrite((ctx, content) -> content.replace("test", "hello"))
                        .build())
                .component(new TestExceptionHandler()) // Support test exception
                // Support web socket
                .component(WebSocketSupport.build(
                        WebSocketServerProtocolConfig.newBuilder().websocketPath("/ws").subprotocols("sp1,sp2").checkStartsWith(true).allowExtensions(true).build(),
                        EchoWebSocketFrameHandler::new))
                .soBackLog(1024).tcpNoDelay() // channel options
                // compression support
                .applyCompressionOptions(HttpContentCompressorProvider.defaultOptions());
        server.defaultHandlerProvider()
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2)) // access logger
                .addLast("/static/auth", new AuthBasic(passwords(), "test")) // HTTP Basic Authentication
                .addLast(new ServeStatic("/static/", "libnetty-example/src/main/resources/static/")) // static resources
                .addLast(new Router().register(controller).register(kotlinController).init()) // router
        ;
        //noinspection DuplicatedCode
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

    private TestHttp2Server() {
    }

}
