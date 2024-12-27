package com.github.fmjsjx.libnetty.handler.ssl;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.pkitesting.CertificateBuilder;
import io.netty.pkitesting.X509Bundle;
import io.netty.util.internal.StringUtil;

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
        var x509Bundle = X509BundleHolder.instance;

        SslContextBuilder builder = SslContextBuilder.forServer(x509Bundle.getKeyPair().getPrivate(), x509Bundle.getCertificatePath());
        try {
            SslContext sslContext = chooseProvider(builder).build();
            return simple(sslContext);
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

    private static final class X509BundleHolder {
        private static final X509Bundle instance;

        static {
            try {
                instance = new CertificateBuilder()
                        .setIsCertificateAuthority(true)
                        .subject("CN=localhost")
                        .notBefore(Instant.ofEpochMilli(System.currentTimeMillis() - 86400000L * 365))
                        .notAfter(Instant.ofEpochMilli(253402300799000L)) // 9999-12-31 23:59:59
                        .algorithm(CertificateBuilder.Algorithm.rsa2048)
                        .buildSelfSigned();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate a self-signed X.509 certificate using CertificateBuilder", e);
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
            SslContext sslContext = chooseProvider(builder).build();
            return simple(sslContext);
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
            SslContext sslContext = chooseProvider(builder).build();
            return simple(sslContext);
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
        return new ServerAutoRebuildSslContextProvider(keyCertChainFile, keyFile, null);
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
    public static final SslContextProvider watchingForClient(File keyCertChainFile, File keyFile)
            throws SSLRuntimeException, IOException {
        return new ClientAutoRebuildSslContextProvider(keyCertChainFile, keyFile, null);
    }

    private static final class ServerAutoRebuildSslContextProvider extends AutoRebuildSslContextProvider {

        private ServerAutoRebuildSslContextProvider(File keyCertChainFile, File keyFile, String keyPassword)
                throws SSLRuntimeException, IOException {
            super(factoryForServer(keyCertChainFile, keyFile, keyPassword),
                    keyCertChainFile.getAbsoluteFile().toPath().getParent(), keyCertChainFile.getName());
        }

    }

    private static final class ClientAutoRebuildSslContextProvider extends AutoRebuildSslContextProvider {

        private ClientAutoRebuildSslContextProvider(File keyCertChainFile, File keyFile, String keyPassword)
                throws SSLRuntimeException, IOException {
            super(factoryForClient(keyCertChainFile, keyFile, keyPassword),
                    keyCertChainFile.getAbsoluteFile().toPath().getParent(), keyCertChainFile.getName());
        }

    }

    private static final SslContextProvider factoryForServer(File keyCertChainFile, File keyFile, String keyPassword) {
        return () -> {
            SslContextBuilder builder = StringUtil.isNullOrEmpty(keyPassword)
                    ? SslContextBuilder.forServer(keyCertChainFile, keyFile)
                    : SslContextBuilder.forServer(keyCertChainFile, keyFile, keyPassword);
            try {
                return chooseProvider(builder).build();
            } catch (SSLException e) {
                throw new SSLRuntimeException("Create SslContext for server failed!", e);
            }
        };
    }

    private static final SslContextProvider factoryForClient(File keyCertChainFile, File keyFile, String keyPassword) {
        return () -> {
            SslContextBuilder builder = SslContextBuilder.forClient();
            if (StringUtil.isNullOrEmpty(keyPassword)) {
                builder.keyManager(keyCertChainFile, keyFile);
            } else {
                builder.keyManager(keyCertChainFile, keyFile, keyPassword);
            }
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
            SslContext sslContext = chooseProvider(builder).build();
            return simple(sslContext);
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
            SslContext sslContext = chooseProvider(builder).build();
            return simple(sslContext);
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

    /**
     * Returns a {@link PermutableSslContextProvider} instance holding the specified
     * {@link SslContext}.
     * 
     * @param sslContext sslContext a {@code SslContext}
     * @return a {@code PermutableSslContextProvider}
     * @since 2.0
     */
    public static final PermutableSslContextProvider permutable(SslContext sslContext) {
        return new PermutableSslContextProviderImpl(sslContext);
    }

    private static final class PermutableSslContextProviderImpl implements PermutableSslContextProvider {

        private final AtomicReference<SslContext> sslContextRef = new AtomicReference<>();

        private PermutableSslContextProviderImpl(SslContext sslContext) {
            set(sslContext);
        }

        @Override
        public SslContext get() {
            return sslContextRef.get();
        }

        @Override
        public SslContext set(SslContext sslContext) {
            if (sslContext == null) {
                throw new IllegalArgumentException("sslContext must not be null");
            }
            return sslContextRef.getAndSet(sslContext);
        }

    }

    private SslContextProviders() {
    }

}
