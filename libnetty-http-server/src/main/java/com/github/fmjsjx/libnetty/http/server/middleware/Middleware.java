package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServer;

/**
 * An interface defines functions which will be applied in HTTP request-response
 * cycle.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface Middleware
        extends BiFunction<HttpRequestContext, MiddlewareChain, CompletionStage<HttpResult>>, AutoCloseable {

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

    /**
     * This method will be invoked by framework when the {@link HttpServer} which
     * this {@link Middleware} belongs to is just be closed.
     * <p>
     * This method is equivalent to {@link #close}.
     * 
     * @throws Exception if any error occurs
     */
    default void onServerClosed() throws Exception {
        close();
    }

    /**
     * Closes this {@link Middleware} and releases any system resources associated
     * with it.
     * <p>
     * Default do nothing.
     * 
     * @throws Exception if any error occurs
     */
    default void close() throws Exception {
        // default do nothing
    }

}
