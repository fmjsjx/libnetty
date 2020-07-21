package com.github.fmjsjx.libnetty.http.client;

import javax.net.ssl.SSLException;

import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * Utility class for {@link SslContext}.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
final class SslContextUtil {

    /**
     * Create and returns a new {@link SslContext} instance for client.
     * 
     * @return a {@link SslContext} for client
     */
    static final SslContext createForClient() {
        try {
            return SslContextBuilder.forClient().build();
        } catch (SSLException e) {
            throw new HttpRuntimeException(e);
        }
    }

    private SslContextUtil() {
    }

}
