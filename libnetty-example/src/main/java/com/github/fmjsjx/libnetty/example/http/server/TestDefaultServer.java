package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpUtil.*;

import com.github.fmjsjx.libnetty.http.HttpUtil;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContextHandler;
import com.github.fmjsjx.libnetty.http.server.SslContextProviders;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.util.CharsetUtil;

public class TestDefaultServer {

    public static void main(String[] args) throws Exception {
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServer server = new DefaultHttpServer("test", SslContextProviders.selfSigned(), 8443)
                .corsConfig(corsConfig).ioThreads(1).maxContentLength(10 * 1024 * 1024).soBackLog(1024).tcpNoDelay();
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
                HttpUtil.contentType(HttpHeaderValues.TEXT_PLAIN, CharsetUtil.UTF_8));
        ChannelFuture future = ctx.writeAndFlush(res);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

}