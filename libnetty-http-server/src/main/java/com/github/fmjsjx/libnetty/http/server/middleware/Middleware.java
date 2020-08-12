package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

/**
 * An interface defines functions which will be applied in HTTP request-response
 * cycle.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Middleware extends BiFunction<HttpRequestContext, MiddlewareChain, CompletionStage<HttpResult>> {

    /**
     * Apply this {@link Middleware} to the given {@link HttpRequestContext}.
     * 
     * @param ctx  the {@code HttpRequestContext} object of the current HTTP request
     * @param next the {@code MiddlewareChain} for invoking the next
     *             {@link Middleware}
     * 
     * @return a {@code CompletionStage<HttpResult>}
     */
    @Override
    CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next);

}
