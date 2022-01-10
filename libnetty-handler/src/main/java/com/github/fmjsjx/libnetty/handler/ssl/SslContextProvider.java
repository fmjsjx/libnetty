package com.github.fmjsjx.libnetty.handler.ssl;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

import io.netty.handler.ssl.SslContext;

/**
 * Provides {@link SslContext} instance..
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface SslContextProvider extends Supplier<SslContext>, Closeable {

    /**
     * Returns the {@link SslContext} instance.
     * 
     * @return a {@code SslContext}
     */
    @Override
    SslContext get();

    /**
     * Close this provider and releases any system resources associated with it.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    default void close() throws IOException {
        // do nothing by default
    }

}