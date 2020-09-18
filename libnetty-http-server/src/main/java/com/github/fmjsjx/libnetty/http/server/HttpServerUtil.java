package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static com.github.fmjsjx.libnetty.http.server.HttpResponseUtil.create;
import static com.github.fmjsjx.libnetty.http.server.HttpResponseUtil.redirect;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
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

    /**
     * Send HTTP response with status {@code 3XX} to client and returns the
     * {@link HttpResult} asynchronously.
     * 
     * @param ctx      the {@link HttpRequestContext}
     * @param location the URI to be redirect to
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> sendRedirect(HttpRequestContext ctx, String location) {
        return sendRedirect(ctx, location, false);
    }

    /**
     * Send HTTP response with status {@code 3XX} to client and returns the
     * {@link HttpResult} asynchronously.
     * 
     * @param ctx      the {@link HttpRequestContext}
     * @param location the URI to be redirect to
     * @param rfc7231  need support {@code RFC 7231} or not
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> sendRedirect(HttpRequestContext ctx, String location,
            boolean rfc7231) {
        FullHttpRequest request = ctx.request();
        HttpVersion version = request.protocolVersion();
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response;
        if (rfc7231) {
            if (version.compareTo(HttpVersion.HTTP_1_1) >= 0) {
                // version >= HTTP/1.1
                if (ctx.method() == HttpMethod.GET) {
                    // Use 303 See Other
                    response = redirect(version, SEE_OTHER, keepAlive, location);
                } else {
                    // Use 307 Temporary Redirect
                    response = redirect(version, TEMPORARY_REDIRECT, keepAlive, location);
                }
            } else {
                // User 302 Found for HTTP/1.0
                response = redirect(version, keepAlive, location);
            }
        } else {
            response = redirect(version, keepAlive, location);
        }
        return HttpServerUtil.sendResponse(ctx, response, 0, keepAlive);
    }

    /**
     * Send HTTP response with {@code "405 Method Not Allowed"} to client and
     * returns the {@link HttpResult} asynchronously.
     * 
     * @param ctx the {@link HttpRequestContext}
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    public static final CompletionStage<HttpResult> sendMethodNotAllowed(HttpRequestContext ctx) {
        return MethodNotAllowed.INSTANCE.respond0(ctx);
    }

    private static final UnpooledByteBufAllocator alloc() {
        return UnpooledByteBufAllocator.DEFAULT;
    }

    private static final class MethodNotAllowed {

        private static final MethodNotAllowed INSTANCE = new MethodNotAllowed();

        private final ByteBuf content;
        private final int length;
        private final AsciiString contentType;

        private MethodNotAllowed() {
            byte[] b = METHOD_NOT_ALLOWED.toString().getBytes();
            content = unreleasableBuffer(alloc().buffer(b.length, b.length).writeBytes(b).asReadOnly());
            length = b.length;
            contentType = contentType(TEXT_PLAIN, CharsetUtil.UTF_8);
        }

        private final CompletionStage<HttpResult> respond0(HttpRequestContext ctx) {
            return respond(ctx, METHOD_NOT_ALLOWED, content.duplicate(), length, contentType);
        }

    }

    private HttpServerUtil() {
    }

}
