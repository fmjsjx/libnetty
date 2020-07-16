package com.github.fmjsjx.libnetty.http.client;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.github.fmjsjx.libnetty.http.client.exception.ClientClosedException;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;

/**
 * Abstract implementation of {@link HttpClient}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class AbstractHttpClient implements HttpClient {

    /**
     * Default is 60 seconds.
     */
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Default is 1 MB.
     */
    protected static final int DEFAULT_MAX_CONTENT_LENGTH = 1 * 1024 * 1024;

    protected final EventLoopGroup group;
    protected final Class<? extends Channel> channelClass;
    protected final SslContext sslContext;
    protected final boolean compressionEnabled;

    private final Object closeLock = new Object();
    protected volatile boolean closed;

    protected AbstractHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContext sslContext,
            boolean compressionEnabled) {
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.channelClass = Objects.requireNonNull(channelClass, "channelClass must not be null");
        this.sslContext = Objects.requireNonNull(sslContext, "sslContext must not be null");
        this.compressionEnabled = compressionEnabled;
    }

    protected EventLoopGroup group() {
        return this.group;
    }

    protected Class<? extends Channel> channelClass() {
        return channelClass;
    }

    @Override
    public SslContext sslContext() {
        return sslContext;
    }

    /**
     * Returns {@code true} if compression feature is enabled of this client.
     * 
     * @return {@code true} if compression feature is enabled
     */
    public boolean compressionEnabled() {
        return compressionEnabled;
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
            throw new ClientClosedException(toString() + " already closed");
        }
    }

    protected boolean isOpen() {
        return !closed;
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler) {
        ensureOpen();
        return sendAsync0(request, contentHandler, Optional.empty());
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler,
            Executor executor) {
        ensureOpen();
        return sendAsync0(request, contentHandler, Optional.of(executor));
    }

    protected abstract <T> CompletableFuture<Response<T>> sendAsync0(Request request,
            HttpContentHandler<T> contentHandler, Optional<Executor> executor);

    /**
     * The abstract implementation of {@link HttpClient.Builder}.
     * 
     * @since 1.0
     *
     * @author MJ Fang
     */
    protected abstract static class AbstractBuilder<C extends HttpClient, Self extends AbstractBuilder<C, ?>>
            implements HttpClient.Builder {

        protected Duration timeout = DEFAULT_TIMEOUT;
        protected int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
        protected SslContext sslContext;
        protected boolean compressionEnabled;

        /**
         * Returns the timeout duration for this client.
         * 
         * @return the timeout {@link Duration}
         */
        public Duration timeout() {
            return timeout;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self timeout(Duration duration) {
            timeout = duration == null ? DEFAULT_TIMEOUT : duration;
            return (Self) this;
        }

        public int timeoutSeconds() {
            return (int) timeout.getSeconds();
        }

        /**
         * Returns the SSL context for this client.
         * 
         * @return the {@link SslContext}
         */
        public SslContext sslContext() {
            return sslContext;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self sslContext(SslContext sslContext) {
            this.sslContext = sslContext;
            return (Self) this;
        }

        protected void ensureSslContext() {
            if (sslContext == null) {
                sslContext = SslContextUtil.createForClient();
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

    }

}
