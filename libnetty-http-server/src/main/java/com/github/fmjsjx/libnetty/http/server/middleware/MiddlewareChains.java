package com.github.fmjsjx.libnetty.http.server.middleware;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.http.server.DefaultHttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * Implementations of {@link MiddlewareChain}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class MiddlewareChains {

    /**
     * Creates and returns a new {@link MiddlewareChain} for common usage.
     * 
     * @param middleware the next {@link Middleware}
     * @param chain      the next {@link MiddlewareChain}
     * @return a {@code MiddlewareChain}
     */
    public static final MiddlewareChain next(Middleware middleware, MiddlewareChain chain) {
        return new DefaultMiddlewareChain(middleware, chain);
    }

    /**
     * Returns a singleton {@link MiddlewareChain} that always returns
     * {@code "404 Not Found"}.
     * 
     * @return a {@code MiddlewareChain}
     */
    public static final MiddlewareChain notFound() {
        return NotFoundMiddlewareChain.INSTANCE;
    }

    /**
     * Creates and returns a new {@link MiddlewareChain} that always returns
     * {@link HttpResponseStatus#NOT_FOUND}.
     * 
     * @param content     the string of HTTP body content
     * @param contentType the content type of the HTTP body
     * @param charset     the {@link Charset} to encode/decode the content
     * 
     * @return a {@code MiddlewareChain}
     */
    public static final MiddlewareChain notFound(String content, CharSequence contentType, Charset charset) {
        return new NotFoundMiddlewareChain(content, contentType, charset);
    }

    private static final class DefaultMiddlewareChain implements MiddlewareChain {

        private final Middleware nextMiddleware;
        private final MiddlewareChain nextChain;

        private DefaultMiddlewareChain(Middleware nextMiddleware, MiddlewareChain nextChain) {
            this.nextMiddleware = nextMiddleware;
            this.nextChain = nextChain;
        }

        @Override
        public CompletionStage<HttpResult> doNext(HttpRequestContext ctx) {
            return nextMiddleware.apply(ctx, nextChain);
        }

    }

    private static final class NotFoundMiddlewareChain implements MiddlewareChain {

        private static final NotFoundMiddlewareChain INSTANCE = new NotFoundMiddlewareChain(NOT_FOUND.toString(),
                HttpHeaderValues.TEXT_PLAIN, CharsetUtil.UTF_8);

        private final ByteBuf contentBuf;
        private final AsciiString contentType;
        private final int contentLength;

        private NotFoundMiddlewareChain(String content, CharSequence contentType, Charset charset) {
            byte[] b = content.getBytes(charset);
            this.contentBuf = Unpooled.unreleasableBuffer(
                    UnpooledByteBufAllocator.DEFAULT.buffer(b.length, b.length).writeBytes(b).asReadOnly());
            this.contentType = contentType(AsciiString.of(contentType), charset);
            this.contentLength = b.length;
        }

        @Override
        public CompletionStage<HttpResult> doNext(HttpRequestContext ctx) {
            FullHttpRequest request = ctx.request();
            HttpVersion version = request.protocolVersion();
            DefaultFullHttpResponse notFound = new DefaultFullHttpResponse(version, NOT_FOUND, contentBuf.duplicate());
            HttpHeaders headers = notFound.headers();
            HttpUtil.setKeepAlive(headers, version, HttpUtil.isKeepAlive(request));
            headers.setInt(HttpHeaderNames.CONTENT_LENGTH, contentLength);
            headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
            CompletableFuture<HttpResult> future = new CompletableFuture<>();
            ctx.channel().writeAndFlush(notFound).addListener(f -> {
                if (f.isSuccess()) {
                    future.complete(new DefaultHttpResult(ctx, contentLength, NOT_FOUND));
                } else if (f.cause() != null) {
                    future.completeExceptionally(f.cause());
                }
            });
            return future;
        }

    }

    private MiddlewareChains() {
    }

}
