package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.INVALID_MESSAGE_TYPE;
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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * Test class for default server.
 */
@Slf4j
public class TestDefaultServer {

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
        TestController controller = new TestController();
        KotlinController kotlinController = new KotlinController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServer server =
                new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(ChannelSslInitializer.of(SslContextProviders.selfSignedForServer())) // SSL
//                .neverTimeout() // never timeout
//                new DefaultHttpServer("test", 8080) // server name and port
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                // support JSON using MixedJsonLibrary
//                .component(MixedJsonLibrary.Builder.recommended().emptyWay(JsonLibrary.EmptyWay.EMPTY).build())
                .component(MixedJsonLibrary.Builder.recommended().emptyWay(JsonLibrary.EmptyWay.EMPTY)
                        .beforeWrite((ctx, content) -> content.replace("test", "hello"))
                        .build())
                .component(new TestExceptionHandler()) // Support test exception
                .component(WebSocketSupport.build(
                        WebSocketServerProtocolConfig.newBuilder().websocketPath("/ws").subprotocols("sp1,sp2").checkStartsWith(true).allowExtensions(true).build(),
                        EchoWebSocketFrameHandler::new
                )) // Support web socket
                .soBackLog(1024).tcpNoDelay() // channel options
                .applyCompressionOptions( // compression support
                        HttpContentCompressorProvider.defaultOptions())
        ;
        server.defaultHandlerProvider() // use default server handler (DefaultHttpServerHandlerProvider)
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

    private TestDefaultServer() {
    }

}

class EchoWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
        System.out.println("-- echo --");
        System.out.println(msg);
        if (msg instanceof TextWebSocketFrame text) {
            System.out.println(text.text());
            ctx.writeAndFlush(new TextWebSocketFrame(text.text()));
        } else {
            ctx.writeAndFlush(new CloseWebSocketFrame(INVALID_MESSAGE_TYPE)).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
            ctx.pipeline().forEach(e -> System.err.println(e.getKey() + " => " + e.getValue()));
            ctx.channel().config().setAutoRead(true);
            System.err.println("sub-protocol: " + handshakeComplete.selectedSubprotocol());
        }
    }

}
