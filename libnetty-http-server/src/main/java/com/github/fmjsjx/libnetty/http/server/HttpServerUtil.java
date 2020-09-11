package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;

/**
 * Utility class for HTTP server.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class HttpServerUtil {

    /**
     * Respond HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx    the {@link HttpRequestContext}
     * @param status the status of the HTTP response to be responded
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respond(HttpRequestContext ctx, HttpResponseStatus status) {
        return respond(ctx, status, CharsetUtil.UTF_8);
    }

    /**
     * Respond HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx     the {@link HttpRequestContext}
     * @param status  the status of the HTTP response to be responded
     * @param charset the {@link Charset} of the response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respond(HttpRequestContext ctx, HttpResponseStatus status,
            Charset charset) {
        FullHttpRequest request = ctx.request();
        FullHttpResponse response = HttpResponseUtil.create(request.protocolVersion(), status, ctx.alloc(), charset,
                HttpUtil.isKeepAlive(request));
        return send(ctx, response);
    }

    /**
     * Respond HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx         the {@link HttpRequestContext}
     * @param status      the status of the HTTP response to be responded
     * @param content     the content of the HTTP response body
     * @param contentType the type of the HTTP response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respond(HttpRequestContext ctx, HttpResponseStatus status,
            ByteBuf content, CharSequence contentType) {
        return respond(ctx, status, content, content.readableBytes(), contentType);
    }

    /**
     * Respond HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx           the {@link HttpRequestContext}
     * @param status        the status of the HTTP response to be responded
     * @param content       the content of the HTTP response body
     * @param contentLength the length of the response body content
     * @param contentType   the type of the HTTP response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respond(HttpRequestContext ctx, HttpResponseStatus status,
            ByteBuf content, int contentLength, CharSequence contentType) {
        FullHttpRequest request = ctx.request();
        FullHttpResponse response = HttpResponseUtil.create(request.protocolVersion(), status, content, contentLength,
                contentType, HttpUtil.isKeepAlive(request));
        return send(ctx, response, contentLength);
    }

    /**
     * Respond HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx           the {@link HttpRequestContext}
     * @param status        the status of the HTTP response to be responded
     * @param content       the content of the HTTP response body
     * @param contentLength the length of the response body content
     * @param contentType   the type of the HTTP response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respond(HttpRequestContext ctx, HttpResponseStatus status,
            ByteBuf content, long contentLength, CharSequence contentType) {
        FullHttpRequest request = ctx.request();
        FullHttpResponse response = HttpResponseUtil.create(request.protocolVersion(), status, content, contentLength,
                contentType, HttpUtil.isKeepAlive(request));
        return send(ctx, response, contentLength);
    }

    /**
     * Send HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx      the {@link HttpRequestContext}
     * @param response the {@link FullHttpResponse}
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static CompletableFuture<HttpResult> send(HttpRequestContext ctx, FullHttpResponse response) {
        return send(ctx, response, response.content().readableBytes());
    }

    /**
     * Send HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx           the {@link HttpRequestContext}
     * @param response      the {@link FullHttpResponse}
     * @param contentLength the length of the response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static CompletableFuture<HttpResult> send(HttpRequestContext ctx, FullHttpResponse response,
            int contentLength) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();
        ctx.channel().writeAndFlush(response).addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, contentLength, response.status()));
            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });
        return future;
    }

    /**
     * Send HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx           the {@link HttpRequestContext}
     * @param response      the {@link FullHttpResponse}
     * @param contentLength the length of the response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static CompletableFuture<HttpResult> send(HttpRequestContext ctx, FullHttpResponse response,
            long contentLength) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();
        ctx.channel().writeAndFlush(response).addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, contentLength, response.status()));
            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });
        return future;
    }

    /**
     * Respond HTTP response typed as {@code "application/json"} to client and
     * returns the {@link HttpResult} asynchronously.
     * 
     * @param ctx     the {@link HttpRequestContext}
     * @param status  the status of the HTTP response to be responded
     * @param content the content of the HTTP response body
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respondJson(HttpRequestContext ctx, HttpResponseStatus status,
            ByteBuf content) {
        return respondJson(ctx, status, content, CharsetUtil.UTF_8);
    }

    /**
     * Respond HTTP response typed as {@code "application/json"} to client and
     * returns the {@link HttpResult} asynchronously.
     * 
     * @param ctx     the {@link HttpRequestContext}
     * @param status  the status of the HTTP response to be responded
     * @param content the content of the HTTP response body
     * @param charset the {@link Charset} of the HTTP response body content
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> respondJson(HttpRequestContext ctx, HttpResponseStatus status,
            ByteBuf content, Charset charset) {
        return respond(ctx, status, content, contentType(APPLICATION_JSON, charset));
    }

    private HttpServerUtil() {
    }

}
