package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

/**
 * A MiddlewareChain is an object giving a view into the invocation chain of an
 * HTTP request. Middlewares use the MiddlewareChain to invoke the next
 * middleware in the chain.
 *
 * @since 1.1
 *
 * @author MJ Fang
 * 
 * @see Middleware
 */
public interface MiddlewareChain extends Function<HttpRequestContext, CompletionStage<HttpResult>> {

    /**
     * Applies this {@link MiddlewareChain} to the given {@link HttpRequestContext}.
     * <p>
     * This method is equivalent to {@link #doNext}.
     * 
     * @param ctx the {@code HttpRequestContext}
     * 
     * @return a {@code CompletionStage<HttpResult>}
     * 
     * @see #doNext
     */
    @Override
    default CompletionStage<HttpResult> apply(HttpRequestContext ctx) {
        return doNext(ctx);
    }

    /**
     * Invoke the next {@link Middleware} in the chain.
     * 
     * @param ctx the {@code HttpRequestContext}
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    CompletionStage<HttpResult> doNext(HttpRequestContext ctx);

}
