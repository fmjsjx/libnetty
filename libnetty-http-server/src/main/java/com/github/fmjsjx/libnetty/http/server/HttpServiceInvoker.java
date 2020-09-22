package com.github.fmjsjx.libnetty.http.server;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Represents a function that invokes an HTTP service.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface HttpServiceInvoker extends Function<HttpRequestContext, CompletionStage<HttpResult>> {

    /**
     * Applies this function to the given context.
     * <p>
     * This method is equivalent to {@link #invoke(HttpRequestContext)}.
     * 
     * @param ctx the context of the HTTP request
     * @return a {@code CompletionStage<HttpResult>}
     */
    @Override
    default CompletionStage<HttpResult> apply(HttpRequestContext ctx) {
        return invoke(ctx);
    }

    /**
     * Invokes an HTTP service with given context.
     * 
     * @param ctx the context of the HTTP request
     * @return a {@code CompletionStage<HttpResult>}
     */
    CompletionStage<HttpResult> invoke(HttpRequestContext ctx);

}
