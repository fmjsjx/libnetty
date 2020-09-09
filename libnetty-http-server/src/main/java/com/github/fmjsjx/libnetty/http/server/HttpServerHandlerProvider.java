package com.github.fmjsjx.libnetty.http.server;

import java.util.function.Supplier;

/**
 * Provides {@link HttpServerHandler}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface HttpServerHandlerProvider extends Supplier<HttpServerHandler>, AutoCloseable {

    /**
     * Returns a {@link HttpServerHandler} instance.
     * 
     * @return a {@code HttpServerHandler}
     */
    @Override
    HttpServerHandler get();

    /**
     * Close this provider and releases any system resources associated with it.
     * 
     * @throws Exception if any error occurs
     */
    @Override
    default void close() throws Exception {
        // default do nothing
    }

}
