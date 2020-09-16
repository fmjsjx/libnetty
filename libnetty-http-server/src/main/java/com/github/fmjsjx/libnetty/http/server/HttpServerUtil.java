package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static com.github.fmjsjx.libnetty.http.server.HttpResponseUtil.create;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.channel.ChannelFutureListener.CLOSE;

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
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = create(request.protocolVersion(), status, ctx.alloc(), charset, keepAlive);
        return sendResponse(ctx, response, response.content().readableBytes(), keepAlive);
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
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        return sendResponse(ctx,
                create(request.protocolVersion(), status, content, contentLength, contentType, keepAlive),
                contentLength, keepAlive);
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
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        return sendResponse(ctx,
                create(request.protocolVersion(), status, content, contentLength, contentType, keepAlive),
                contentLength, keepAlive);
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
    public static CompletableFuture<HttpResult> sendResponse(HttpRequestContext ctx, FullHttpResponse response) {
        return sendResponse(ctx, response, response.content().readableBytes(), HttpUtil.isKeepAlive(response));
    }

    /**
     * Send HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx           the {@link HttpRequestContext}
     * @param response      the {@link FullHttpResponse}
     * @param contentLength the length of the response body content
     * @param keepAlive     if the connection is {@code keep-alive} or not
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static CompletableFuture<HttpResult> sendResponse(HttpRequestContext ctx, FullHttpResponse response,
            int contentLength, boolean keepAlive) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();
        ChannelFuture sendFuture = ctx.channel().writeAndFlush(response);
        sendFuture.addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, contentLength, response.status()));
            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });
        if (!keepAlive) {
            sendFuture.addListener(CLOSE);
        }
        return future;
    }

    /**
     * Send HTTP response to client and returns the {@link HttpResult}
     * asynchronously.
     * 
     * @param ctx           the {@link HttpRequestContext}
     * @param response      the {@link FullHttpResponse}
     * @param contentLength the length of the response body content
     * @param keepAlive     if the connection is {@code keep-alive} or not
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static CompletableFuture<HttpResult> sendResponse(HttpRequestContext ctx, FullHttpResponse response,
            long contentLength, boolean keepAlive) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();
        ChannelFuture sendFuture = ctx.channel().writeAndFlush(response);
        sendFuture.addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, contentLength, response.status()));
            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });
        if (!keepAlive) {
            sendFuture.addListener(CLOSE);
        }
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
