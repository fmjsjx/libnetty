package com.github.fmjsjx.libnetty.http.client;

import javax.net.ssl.SSLException;

import com.github.fmjsjx.libnetty.http.client.exception.HttpRuntimeException;

import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for {@link SslContext}.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SslContextUtil {

    /**
     * Create and returns a new {@link SslContext} instance for client.
     * 
     * @return a {@link SslContext} for client
     */
    public static final SslContext createForClient() {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();
            if (OpenSsl.isAvailable()) {
                builder.sslProvider(SslProvider.OPENSSL_REFCNT);
            }
            return builder.build();
        } catch (SSLException e) {
            throw new HttpRuntimeException(e);
        }
    }

}
