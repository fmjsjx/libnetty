package com.github.fmjsjx.libnetty.handler.ssl;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Implementations of {@link SslContextProvider}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class SslContextProviders {

    /**
     * Returns a simple implementation of {@link SslContextProvider} which holding a
     * self-signed certificate {@code SslContext} for server.
     * 
     * @return a {@code SslContextProvider} holding a self-signed certificate
     *         {@code SslContext} for server
     * 
     * @throws SSLRuntimeException if any SSL error occurs
     */
    public static final SslContextProvider selfSignedForServer() throws SSLRuntimeException {
        SelfSignedCertificate ssc = SelfSignedCertificateHolder.instance;
        try {
            SslContextBuilder builder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
            if (OpenSsl.isAvailable()) {
                builder.sslProvider(SslProvider.OPENSSL_REFCNT);
            }
            SslContext sslContex = builder.build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new SSLRuntimeException("Create self-signed certificate SslContext failed!", e);
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
