package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpUtil.setKeepAlive;

import java.io.File;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.HttpCommonUtil;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContextHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.util.CharsetUtil;

/**
 * Test server with watching SSL.
 */
public class TestWatchingSslServer {

    private static final String sslCertPath = System.getProperty("ssl.certFile",
            "src/main/resources/ssl/localhost.crt");
    private static final String sslKeyFile = System.getProperty("ssl.keyFile", "libnetty-example/src/main/resources/ssl/localhost.key");

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        File keyCertChainFile = new File(sslCertPath);
        File keyFile = new File(sslKeyFile);
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        try (SslContextProvider sslContextProvider = SslContextProviders.watchingForServer(keyCertChainFile, keyFile)) {
            DefaultHttpServer server = new DefaultHttpServer("test", ChannelSslInitializer.of(sslContextProvider), 8443)
                    .corsConfig(corsConfig).ioThreads(1).maxContentLength(10 * 1024 * 1024).soBackLog(1024)
                    .tcpNoDelay();
            server.handler(new TestHandler());
            try {
                server.startup();
                //noinspection ResultOfMethodCallIgnored
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

@Sharable
class TestHandler extends HttpRequestContextHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpRequestContext msg) throws Exception {
        System.out.println("-- test --");
        System.out.println(msg);
        System.out.println();
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeUtf8(ctx.alloc(), "200 OK");
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(msg.request().protocolVersion(),
                HttpResponseStatus.OK, body);
        setContentLength(res, body.readableBytes());
        boolean keepAlive = isKeepAlive(msg.request());
        setKeepAlive(res, keepAlive);
        res.headers().set(HttpHeaderNames.CONTENT_TYPE,
                HttpCommonUtil.contentType(HttpHeaderValues.TEXT_PLAIN, CharsetUtil.UTF_8));
        ChannelFuture future = ctx.writeAndFlush(res);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

}