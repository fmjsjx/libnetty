package com.github.fmjsjx.libnetty.http.server.middleware;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.nio.charset.Charset;
import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
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
            return HttpServerUtil.respond(ctx, NOT_FOUND, contentBuf.duplicate(), contentLength, contentType);
        }

    }

    private MiddlewareChains() {
    }

}
