package com.github.fmjsjx.libnetty.handler.ssl;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
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
     * self-signed certificate {@link SslContext} for server.
     * 
     * @return a {@code SslContextProvider} holding a self-signed certificate
     *         {@code SslContext} for server
     * 
     * @throws SSLRuntimeException if any SSL error occurs
     */
    public static final SslContextProvider selfSignedForServer() throws SSLRuntimeException {
        SelfSignedCertificate ssc = SelfSignedCertificateHolder.instance;
        SslContextBuilder builder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
        try {
            SslContext sslContex = chooseProvider(builder).build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new SSLRuntimeException("Create self-signed certificate SslContext failed!", e);
        }
    }

    private static final SslContextBuilder chooseProvider(SslContextBuilder builder) {
        if (OpenSsl.isAvailable()) {
            builder.sslProvider(SslProvider.OPENSSL_REFCNT);
        }
        return builder;
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
     * Returns a simple implementation of {@link SslContextProvider} which holding a
     * {@code server-side} {@link SslContext}.
     * 
     * @param keyCertChainFile an X.509 certificate chain file in PEM format
     * @param keyFile          a PKCS#8 private key file in PEM format
     * @return a {@code SslContextProvider}
     * @throws SSLRuntimeException if any SSL error occurs
     * @see SslContextProviders#forServer(File, File, String)
     */
    public static final SslContextProvider forServer(File keyCertChainFile, File keyFile) throws SSLRuntimeException {
        SslContextBuilder builder = SslContextBuilder.forServer(keyCertChainFile, keyFile);
        try {
            SslContext sslContex = chooseProvider(builder).build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new SSLRuntimeException("Create SslContext for server failed!", e);
        }
    }

    /**
     * Returns a simple implementation of {@link SslContextProvider} which holding a
     * {@code server-side} {@link SslContext}.
     * 
     * @param keyCertChainFile an X.509 certificate chain file in PEM format
     * @param keyFile          a PKCS#8 private key file in PEM format
     * @param keyPassword      the password of the {@code keyFile}, or {@code null}
     *                         if it's not password-protected
     * @return a {@code SslContextProvider}
     * @throws SSLRuntimeException if any SSL error occurs
     * @see #forServer(File, File)
     */
    public static final SslContextProvider forServer(File keyCertChainFile, File keyFile, String keyPassword)
            throws SSLRuntimeException {
        SslContextBuilder builder = SslContextBuilder.forServer(keyCertChainFile, keyFile, keyPassword);
        try {
            SslContext sslContex = chooseProvider(builder).build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new SSLRuntimeException("Create SslContext for server failed!", e);
        }
    }

    /**
     * Returns an {@link SslContextProvider} which will auto rebuild
     * {@link SslContext} when the watching certificate file just be modified.
     * 
     * @param keyCertChainFile an X.509 certificate chain file in PEM format
     * @param keyFile          a PKCS#8 private key file in PEM format
     * 
     * @return a {@code SslContextProvider}
     * @throws SSLRuntimeException if any SSL error occurs
     * @throws IOException         if any IO error occurs
     */
    public static final SslContextProvider watchingForServer(File keyCertChainFile, File keyFile)
            throws SSLRuntimeException, IOException {
        return new ServerAutoRebuildSslContextProvider(keyCertChainFile, keyFile);
    }

    private static final class ServerAutoRebuildSslContextProvider extends AutoRebuildSslContextProvider {

        private ServerAutoRebuildSslContextProvider(File keyCertChainFile, File keyFile)
                throws SSLRuntimeException, IOException {
            super(factoryForServer(keyCertChainFile, keyFile), keyCertChainFile.getAbsoluteFile().toPath().getParent(),
                    keyCertChainFile.getName());
        }

    }

    private static final SslContextProvider factoryForServer(File keyCertChainFile, File keyFile) {
        return () -> {
            SslContextBuilder builder = SslContextBuilder.forServer(keyCertChainFile, keyFile);
            try {
                return chooseProvider(builder).build();
            } catch (SSLException e) {
                throw new SSLRuntimeException("Create SslContext for server failed!", e);
            }
        };
    }

    /**
     * Returns a simple implementation of {@link SslContextProvider} which holding
     * an insecure {@link SslContext} for client.
     * 
     * @return a {@code SslContextProvider} holding insecure {@code SslContext} for
     *         client
     * @throws SSLRuntimeException if any SSL error occurs
     */
    public static final SslContextProvider insecureForClient() throws SSLRuntimeException {
        SslContextBuilder builder = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE);
        try {
            SslContext sslContex = chooseProvider(builder).build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new SSLRuntimeException("Create insecure SslContext for client failed!", e);
        }
    }

    /**
     * Returns a simple implementation of {@link SslContextProvider} which holding
     * an {@link SslContext} trusted certificates for verifying the remote
     * endpoint's certificate.
     * 
     * <p>
     * The file should contain an X.509 certificate collection in PEM format.
     * 
     * @param trustCertCollectionFile file contains an X.509 certificate collection
     *                                in PEM format
     * @return a {@code SslContextProvider}
     * @throws SSLRuntimeException if any SSL error occurs
     */
    public static final SslContextProvider forClient(File trustCertCollectionFile) throws SSLRuntimeException {
        SslContextBuilder builder = SslContextBuilder.forClient().trustManager(trustCertCollectionFile);
        try {
            SslContext sslContex = chooseProvider(builder).build();
            return simple(sslContex);
        } catch (SSLException e) {
            throw new SSLRuntimeException("Create SslContext with certificate failed!", e);
        }
    }

    /**
     * Returns a simple implementation of {@link SslContextProvider} which just
     * holding the specified {@link SslContext}.
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
