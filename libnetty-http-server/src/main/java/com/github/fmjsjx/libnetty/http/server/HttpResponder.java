package com.github.fmjsjx.libnetty.http.server;

import java.util.function.Function;

/**
 * An HTTP responder responds HTTP response to clients and returns the
 * {@link HttpResult}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface HttpResponder extends Function<HttpRequestContext, HttpResult> {

    /**
     * Applies this {@link HttpResponder} to the given {@link HttpRequestContext}.
     * <p>
     * This method is equivalent to {@link #respond}.
     * 
     * @param ctx a {@code HttpRequestContext}
     * 
     * @return a {@code HttpResult}
     * 
     * @see #respond
     */
    @Override
    default HttpResult apply(HttpRequestContext ctx) {
        return respond(ctx);
    }

    /**
     * Responds HTTP response to the client, holding by the specified
     * {@link HttpRequestContext}, and then returns the {@link HttpResult}.
     * 
     * @param ctx a {@code HttpRequestContext}
     * 
     * @return a {@code HttpResult}
     */
    HttpResult respond(HttpRequestContext ctx);

}
