package com.github.fmjsjx.libnetty.http.server.middleware;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

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

        private static final NotFoundMiddlewareChain INSTANCE = new NotFoundMiddlewareChain();

        @Override
        public CompletionStage<HttpResult> doNext(HttpRequestContext ctx) {
            return ctx.simpleRespond(NOT_FOUND);
        }

    }

    private MiddlewareChains() {
    }

}
