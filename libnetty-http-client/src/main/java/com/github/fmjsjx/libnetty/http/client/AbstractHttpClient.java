package com.github.fmjsjx.libnetty.http.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.exception.ClientClosedException;

import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.NettyRuntime;

/**
 * Abstract implementation of {@link HttpClient}.
 *
 * @since 1.0
 *
 * @author MJ Fang
 *
 * @see DefaultHttpClient
 * @see SimpleHttpClient
 */
public abstract class AbstractHttpClient implements HttpClient {

    /**
     * Default is 60 seconds.
     */
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Default is 16 MB.
     */
    protected static final int DEFAULT_MAX_CONTENT_LENGTH = 16 * 1024 * 1024;

    protected final EventLoopGroup group;
    protected final Class<? extends Channel> channelClass;
    protected final SslContextProvider sslContextProvider;
    protected final boolean compressionEnabled;
    protected final Optional<ProxyHandlerFactory<? extends ProxyHandler>> proxyHandlerFactory;
    protected final Optional<Duration> defaultRequestTimeout;

    private final Object closeLock = new Object();
    protected volatile boolean closed;

    protected AbstractHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass,
            SslContextProvider sslContextProvider, boolean compressionEnabled,
            ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory, Duration defaultRequestTimeout) {
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.channelClass = Objects.requireNonNull(channelClass, "channelClass must not be null");
        this.sslContextProvider = Objects.requireNonNull(sslContextProvider, "sslContextProvider must not be null");
        this.compressionEnabled = compressionEnabled;
        this.proxyHandlerFactory = Optional.ofNullable(proxyHandlerFactory);
        this.defaultRequestTimeout = Optional.ofNullable(defaultRequestTimeout);
    }

    protected EventLoopGroup group() {
        return this.group;
    }

    protected Class<? extends Channel> channelClass() {
        return channelClass;
    }

    @Override
    public SslContextProvider sslContextProvider() {
        return sslContextProvider;
    }

    /**
     * Returns {@code true} if compression feature is enabled of this client.
     *
     * @return {@code true} if compression feature is enabled
     */
    public boolean compressionEnabled() {
        return compressionEnabled;
    }

    /**
     * Returns the default request timeout duration of this client.
     *
     * @return an {@code Optional<Duration>}
     */
    public Optional<Duration> defaultRequestTimeout() {
        return defaultRequestTimeout;
    }

    @Override
    public void close() {
        if (isOpen()) {
            boolean doClose = false;
            synchronized (closeLock) {
                if (isOpen()) {
                    closed = true;
                    doClose = true;
                }
            }
            if (doClose) {
                close0();
            }
        }
    }

    protected abstract void close0();

    protected void ensureOpen() {
        if (closed) {
            throw newClientClosed();
        }
    }

    protected final ClientClosedException newClientClosed() {
        return new ClientClosedException(toString() + " already closed");
    }

    protected boolean isOpen() {
        return !closed;
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler) {
        if (closed) {
            return CompletableFuture.failedFuture(newClientClosed());
        }
        return doSendAsync(request, contentHandler, Optional.empty());
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler,
            Executor executor) {
        if (closed) {
            return CompletableFuture.supplyAsync(() -> {
                throw newClientClosed();
            }, executor);
        }
        return doSendAsync(request, contentHandler, Optional.of(executor));
    }

    private <T> CompletableFuture<Response<T>> doSendAsync(Request request, HttpContentHandler<T> contentHandler,
                                                           Optional<Executor> executor) {
        var requestTimeout = requestTimeout(request);
        var future = sendAsync0(request, contentHandler, executor);
        if (requestTimeout.isEmpty()) {
            return future;
        }
        return future.orTimeout(requestTimeout.get().toNanos(), TimeUnit.NANOSECONDS);
    }

    protected abstract <T> CompletableFuture<Response<T>> sendAsync0(Request request,
            HttpContentHandler<T> contentHandler, Optional<Executor> executor);

    protected Optional<Duration> requestTimeout(Request request) {
        return request.timeout().or(this::defaultRequestTimeout);
    }

    @Override
    public <T> Response<T> send(Request request, HttpContentHandler<T> contentHandler) throws IOException,
            InterruptedException, HttpRuntimeException, TimeoutException {
        ensureOpen();
        try {
            var requestTimeout = requestTimeout(request);
            var future = sendAsync0(request, contentHandler, Optional.empty());
            if (requestTimeout.isEmpty()) {
                return future.get();
            }
            return future.get(requestTimeout.get().toNanos(), TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new HttpRuntimeException(cause);
            }
        }
    }

    /**
     * The abstract implementation of {@link HttpClient.Builder}.
     *
     * @since 1.0
     *
     * @author MJ Fang
     */
    protected abstract static class AbstractBuilder<C extends HttpClient, Self extends AbstractBuilder<C, ?>>
            implements HttpClient.Builder {

        protected int ioThreads = NettyRuntime.availableProcessors();
        protected Duration connectionTimeout = DEFAULT_TIMEOUT;
        protected Duration requestTimeout;
        protected int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
        protected SslContextProvider sslContextProvider;
        protected boolean compressionEnabled;
        protected ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory;

        /**
         * Returns the number of IO threads for this client.
         *
         * @return the number of IO threads for this client
         */
        public int ioThreads() {
            return ioThreads;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self ioThreads(int ioThreads) {
            this.ioThreads = Math.max(0, ioThreads);
            return (Self) this;
        }

        /**
         * Returns the timeout duration for this client.
         *
         * @return the timeout {@link Duration}
         * @deprecated please use {@link #connectionTimeout()} instead
         */
        @Deprecated
        public Duration timeout() {
            return connectionTimeout();
        }

        @Override
        @SuppressWarnings("deprecation")
        public Self timeout(Duration duration) {
            return connectionTimeout(duration);
        }

        /**
         * Returns the connection timeout duration for this client.
         *
         * @return the connection timeout {@link Duration}
         */
        public Duration connectionTimeout() {
            return connectionTimeout;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self connectionTimeout(Duration duration) {
            connectionTimeout = duration == null ? DEFAULT_TIMEOUT : duration;
            return (Self) this;
        }

        /**
         * Returns the request timeout duration for this client.
         *
         * @return the request timeout {@link Duration}
         */
        public Duration requestTimeout() {
            return requestTimeout;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self requestTimeout(Duration duration) {
            requestTimeout = duration;
            return (Self) this;
        }

        /**
         * Returns the timeout seconds for this client.
         *
         * @return the timeout seconds
         * @deprecated please use {@link #connectionTimeoutSeconds()} instead
         */
        @Deprecated
        public int timeoutSeconds() {
            return connectionTimeoutSeconds();
        }

        /**
         * Returns the connection timeout seconds for this client.
         *
         * @return the connection timeout
         * @since 2.5
         */
        public int connectionTimeoutSeconds() {
            return (int) connectionTimeout().getSeconds();
        }

        /**
         * Returns the SSL context for this client.
         *
         * @return the {@link SslContext}
         * @deprecated please use
         */
        @Deprecated
        public SslContext sslContext() {
            return sslContextProvider != null ? sslContextProvider.get() : null;
        }

        /**
         * Returns the {@link SslContextProvider}.
         *
         * @return a {@code SslContextProvider}
         * @since 1.1
         */
        public SslContextProvider sslContextProvider() {
            return sslContextProvider;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self sslContextProvider(SslContextProvider sslContextProvider) {
            this.sslContextProvider = sslContextProvider;
            return (Self) this;
        }

        protected void ensureSslContext() {
            if (sslContextProvider == null) {
                sslContextProvider = SslContextProviders.simple(SslContextUtil.createForClient());
            }
        }

        /**
         * Returns the max content length for this client.
         *
         * @return the max content length
         */
        public int maxContentLength() {
            return maxContentLength;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self maxContentLength(int maxContentLength) {
            if (maxContentLength > 0) {
                this.maxContentLength = maxContentLength;
            }
            return (Self) this;
        }

        @Override
        public Self enableCompression() {
            return compression(true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self compression(boolean enabled) {
            this.compressionEnabled = enabled;
            return (Self) this;
        }

        /**
         * Returns {@code true} if the compression feature is enabled.
         *
         * @return {@code true} if the compression feature is enabled
         */
        public boolean compressionEnabled() {
            return compressionEnabled;
        }

        @SuppressWarnings("deprecation")
        @Override
        @Deprecated
        public Self enableBrotli() {
            return brotli(true);
        }

        @Override
        @SuppressWarnings({"unchecked", "deprecation"})
        @Deprecated
        public Self brotli(boolean enabled) {
            this.compressionEnabled = enabled;
            return (Self) this;
        }

        /**
         * Returns {@code true} if Brotli is enabled.
         *
         * @return {@code true} if Brotli is enabled
         * @deprecated use {@link Brotli#isAvailable()} instead
         */
        @Deprecated
        public boolean brotliEnabled() {
            return compressionEnabled() && Brotli.isAvailable();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self proxyHandlerFactory(ProxyHandlerFactory<? extends ProxyHandler> factory) {
            this.proxyHandlerFactory = factory;
            return (Self) this;
        }

        /**
         * Returns the factory of {@link ProxyHandler}.
         *
         * @return the factory of {@code ProxyHandler}
         *
         * @since 1.2
         */
        public ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory() {
            return this.proxyHandlerFactory;
        }

    }

}
