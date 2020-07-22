package com.github.fmjsjx.libnetty.http.server;

import javax.net.ssl.SSLException;

import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Implementations of {@link SslContextProvider}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class SslContextProviders {

    /**
     * Returns a simple implementation of {@link SslContextProvider} which holding a
     * self-signed certificate {@code SslContext}.
     * 
     * @return a {@code SslContextProvider} holding a self-signed certificate
     *         {@code SslContext}.
     */
    public static final SslContextProvider selfSigned() {
        SelfSignedCertificate ssc = SelfSignedCertificateHolder.instance;
        try {
            SslContext sslContex = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new HttpRuntimeException("Create self-signed certificate SslContext failed!", e);
        }
    }

    private static final class SelfSignedCertificateHolder {

        private static final SelfSignedCertificate instance;

        static {
            try {
                instance = new SelfSignedCertificate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns a simple implementation of {@link SslContextProvider} which just
     * holding the specified {@code sslContext}.
     * 
     * @param sslContext a {@code SslContext}
     * @return a {@code SslContextProvider}
     */
    public static final SslContextProvider simple(SslContext sslContext) {
        return () -> sslContext;
    }

    private SslContextProviders() {
    }

}
