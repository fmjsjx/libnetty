package com.github.fmjsjx.libnetty.handler.ssl;

import java.util.function.Function;

import io.netty.handler.ssl.SslContext;

/**
 * Provides setter functions for {@link SslContextProvider}.
 * 
 * @since 2.0
 *
 * @author MJ Fang
 * 
 * @see SslContextProvider
 */
public interface PermutableSslContextProvider extends SslContextProvider, Function<SslContext, SslContext> {

    @Override
    default SslContext apply(SslContext sslContext) {
        return set(sslContext);
    }

    /**
     * Set the holding {@link SslContext} to {@code sslContext} and returns the old
     * value.
     * 
     * @param sslContext the new {@link SslContext}
     * @return the old {@link SslContext}
     */
    SslContext set(SslContext sslContext);

}
