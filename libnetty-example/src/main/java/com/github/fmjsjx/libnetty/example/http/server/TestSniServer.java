package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.handler.ssl.SniHandlerProviders;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.middleware.ServeStatic;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.LogFormat;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.Slf4jLoggerWrapper;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.util.DomainWildcardMappingBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Test class for server with SNI feature.
 */
@Slf4j
public class TestSniServer {

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        TestController controller = new TestController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        //noinspection resource
        var mappingBuilder = new DomainWildcardMappingBuilder<>(SslContextProviders.selfSignedForServer().get());
        var sniHandlerProvider = SniHandlerProviders.permutable(mappingBuilder.build());
        DefaultHttpServer server = new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(ChannelSslInitializer.of(sniHandlerProvider)) // SSL
                .neverTimeout() // never timeout
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                .supportJson() // Support JSON using embedded json library
                .component(new TestExceptionHandler()) // Support test exception
                .soBackLog(1024).tcpNoDelay() // channel options
                .applyCompressionOptions( // compression support
                        HttpContentCompressorProvider.defaultOptions());
        server.defaultHandlerProvider() // use default server handler (DefaultHttpServerHandlerProvider)
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2)) // access logger
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
