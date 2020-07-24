package com.github.fmjsjx.libnetty.http.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Provides {@link HttpServerHandler}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface HttpServerHandlerProvider extends Supplier<HttpServerHandler>, Closeable {

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
     * @throws IOException if an I/O error occurs
     */
    @Override
    default void close() throws IOException {
        // default do nothing
    }

}
