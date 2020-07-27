package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

import java.io.File;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;

public class TestWatchingSslServer {

    private static final String sslCertPath = System.getProperty("ssl.certFile",
            "src/main/resources/ssl/localhost.crt");
    private static final String sslKeyFile = System.getProperty("ssl.keyFile", "src/main/resources/ssl/localhost.key");

    public static void main(String[] args) throws Exception {
        File keyCertChainFile = new File(sslCertPath);
        File keyFile = new File(sslKeyFile);
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        try (SslContextProvider sslContextProvider = SslContextProviders.watchingForServer(keyCertChainFile, keyFile)) {
            DefaultHttpServer server = new DefaultHttpServer("test", sslContextProvider, 8443).corsConfig(corsConfig)
                    .ioThreads(1).maxContentLength(10 * 1024 * 1024).soBackLog(1024).tcpNoDelay();
            server.handler(new TestHandler());
            try {
                server.startup();
                System.in.read();
            } catch (Exception e) {
                System.err.println("Unexpected error occurs when startup " + server);
                e.printStackTrace();
            } finally {
                if (server.isRunning()) {
                    server.shutdown();
                }
            }
        }
    }

}
