package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

import java.util.Collections;
import java.util.Map;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorFactory;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.component.DefaultWorkerPool;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.LogFormat;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.Slf4jLoggerWrapper;
import com.github.fmjsjx.libnetty.http.server.middleware.AuthBasic;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.middleware.ServeStatic;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestBlockingServer {

    private static final Map<String, String> passwds() {
        return Collections.singletonMap("test", "123456");
    }

    public static void main(String[] args) throws Exception {
        BlockingTestController controller = new BlockingTestController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServer server = new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(SslContextProviders.selfSignedForServer()) // SSL
                .neverTimeout() // never timeout
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                .supportJson() // Support JSON using Jackson2s
                .component(new DefaultWorkerPool(1, 1)) // support blocking APIs
                .soBackLog(1024).tcpNoDelay() // channel options
                .applyCompressionSettings( // compression support
                        HttpContentCompressorFactory.defaultSettings()) // default settings
//                        b -> b.compressionLevel(1).memLevel(1).windowBits(9).contentSizeThreshold(4096)) // fastest
//                        b -> b.compressionLevel(9).memLevel(9).windowBits(15).contentSizeThreshold(512)) // best
        ;
        server.defaultHandlerProvider() // use default server handler (DefaultHttpServerHandlerProvider)
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2)) // access logger
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

