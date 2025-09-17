package com.github.fmjsjx.libnetty.http.client;

import static com.github.fmjsjx.libnetty.http.HttpCommonUtil.contentType;
import static com.github.fmjsjx.libnetty.http.client.AbstractHttpClient.AcceptEncodingValues.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.netty.handler.codec.http.HttpHeaderValues.GZIP_DEFLATE;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpMethod.DELETE;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.exception.ClientClosedException;

import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.Zstd;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.ErrorDataEncoderException;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.AsciiString;
import io.netty.util.NettyRuntime;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link HttpClient}.
 *
 * @author MJ Fang
 * @see DefaultHttpClient
 * @see SimpleHttpClient
 * @since 1.0
 */
public abstract class AbstractHttpClient implements HttpClient {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Default is 60 seconds.
     */
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Default is 16 MB.
     */
    protected static final int DEFAULT_MAX_CONTENT_LENGTH = 16 * 1024 * 1024;

    /**
     * Values of HTTP header {@code accept-encoding}.
     *
     * @since 3.8
     */
    protected static final class AcceptEncodingValues {
        /**
         * {@code "gzip,deflate,br"}
         *
         * @since 3.8
         */
        @SuppressWarnings("deprecation")
        public static final AsciiString GZIP_DEFLATE_BR = HttpClient.GZIP_DEFLATE_BR;
        /**
         * {@code "gzip,deflate,zstd"}
         *
         * @since 3.8
         */
        public static final AsciiString GZIP_DEFLATE_ZSTD = AsciiString.cached("gzip,deflate,zstd");
        /**
         * {@code "gzip,deflate,br,zstd"}
         *
         * @since 3.8
         */
        public static final AsciiString GZIP_DEFLATE_BR_ZSTD = AsciiString.cached("gzip,deflate,br,zstd");
    }

    protected static final AsciiString DEFAULT_USER_AGENT_VALUE = AsciiString.cached("Libnetty/3.9.1-SNAPSHOT");

    protected final EventLoopGroup group;
    protected final Class<? extends Channel> channelClass;
    protected final SslContextProvider sslContextProvider;
    protected final boolean compressionEnabled;
    protected final boolean autoDecompression;
    protected final Optional<ProxyHandlerFactory<? extends ProxyHandler>> proxyHandlerFactory;
    protected final Optional<Duration> defaultRequestTimeout;
    protected final Optional<CharSequence> defaultUserAgent;

    private final Object closeLock = new Object();
    protected volatile boolean closed;

    protected AbstractHttpClient(
            EventLoopGroup group, Class<? extends Channel> channelClass, SslContextProvider sslContextProvider,
            boolean compressionEnabled, ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory,
            Duration defaultRequestTimeout, CharSequence defaultUserAgent) {
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.channelClass = Objects.requireNonNull(channelClass, "channelClass must not be null");
        this.sslContextProvider = Objects.requireNonNull(sslContextProvider, "sslContextProvider must not be null");
        this.compressionEnabled = compressionEnabled;
        // autoDecompression is always be true now
        autoDecompression = true;
        this.proxyHandlerFactory = Optional.ofNullable(proxyHandlerFactory);
        this.defaultRequestTimeout = Optional.ofNullable(defaultRequestTimeout);
        this.defaultUserAgent = Optional.ofNullable(defaultUserAgent);
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
        //noinspection UnnecessaryToStringCall
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

    protected abstract <T> CompletableFuture<Response<T>> sendAsync0(
            Request request, HttpContentHandler<T> contentHandler, Optional<Executor> executor);

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

    protected HttpRequest createHttpRequest(ByteBufAllocator alloc, Request request, CharSequence headerHost,
                                            String requestUri, boolean keepAlive) {
        HttpMethod method = request.method();
        HttpHeaders headers = request.headers();
        if (!headers.contains(HOST)) {
            headers.set(HOST, headerHost);
        }
        if (!headers.contains(USER_AGENT) && defaultUserAgent.isPresent()) {
            headers.set(USER_AGENT, defaultUserAgent.get());
        }
        HttpRequest req;
        if (request.multipartBody().isPresent()) {
            req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, requestUri, headers);
        } else {
            var content = request.contentHolder().content(alloc);
            req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, requestUri, content,
                    headers, request.trailingHeaders());
            if (method == POST || method == PUT || method == PATCH || (method == DELETE && content.isReadable())) {
                int contentLength = content.readableBytes();
                headers.setInt(CONTENT_LENGTH, contentLength);
                if (!headers.contains(CONTENT_TYPE)) {
                    headers.set(CONTENT_TYPE, contentType(APPLICATION_X_WWW_FORM_URLENCODED));
                }
            }
        }
        if (!headers.contains(ACCEPT_ENCODING)) {
            if (compressionEnabled) {
                if (Brotli.isAvailable()) {
                    headers.set(ACCEPT_ENCODING, Zstd.isAvailable() ? GZIP_DEFLATE_BR_ZSTD : AcceptEncodingValues.GZIP_DEFLATE_BR);
                } else {
                    headers.set(ACCEPT_ENCODING, Zstd.isAvailable() ? GZIP_DEFLATE_ZSTD : GZIP_DEFLATE);
                }
            }
        }
        HttpUtil.setKeepAlive(req, keepAlive);
        return req;
    }

    protected HttpPostRequestEncoder createHttpPostRequestEncoder(
            HttpDataFactory factory, HttpRequest request, MultipartBody body) {
        try {
            return new HttpPostRequestEncoder(factory, request, true);
        } catch (ErrorDataEncoderException e) {
            throw new HttpRuntimeException(e);
        }
    }

    protected HttpDataFactory createHttpDataFactory(MultipartBody body) {
        HttpDataFactory factory;
        if (body.entries().stream().anyMatch(e -> e instanceof FileUploadEntry)) {
            factory = new DefaultHttpDataFactory(body.charset());
        } else {
            factory = new DefaultHttpDataFactory(false, body.charset());
        }
        return factory;
    }

    FileUpload createFileUpload(HttpRequest request, HttpDataFactory factory, Charset charset,
                                Channel channel, ContentProviderFileUploadEntry entry) {
        var content = entry.contentProvider().apply(channel.alloc());
        var fileUpload = factory.createFileUpload(request, entry.name(), entry.filename(), entry.contentType(),
                "binary", charset, content.readableBytes());
        try {
            fileUpload.setContent(content);
        } catch (IOException e) {
            // Can't reach this line commonly
            content.release();
            throw new HttpRuntimeException(e);
        }
        return fileUpload;
    }

    protected void sendHttpRequest(HttpRequest req, Channel channel, Request request) {
        if (request.multipartBody().isPresent()) {
            var body = request.multipartBody().get();
            var factory = createHttpDataFactory(body);
            var encoder = createHttpPostRequestEncoder(factory, req, body);
            try {
                for (var entry : body.entries()) {
                    if (entry instanceof ContentProviderFileUploadEntry fileUploadEntry) {
                        var httpData = createFileUpload(req, factory, body.charset(), channel, fileUploadEntry);
                        body.getToDeleteDataList(true).add(httpData);
                        encoder.addBodyHttpData(httpData);
                    } else {
                        entry.addBody(encoder);
                    }
                }
                var freq = encoder.finalizeRequest();
                log.debug("Send HTTP request with multipart/form-data data async: {}", req);
                log.debug("-- headers : {}", freq.headers());
                channel.write(freq);
                channel.write(encoder).addListener(cf -> {
                    if (cf.isDone()) {
                        encoder.cleanFiles();
                        // HttpPostRequestEncoder never releases ByteBuf in cleanFiles()
                        // when the useDisk on factory is false, just safe release them here
                        safeRelease(body);
                    }
                });
                channel.flush();
            } catch (ErrorDataEncoderException e) {
                encoder.cleanFiles();
                // HttpPostRequestEncoder never releases ByteBuf in cleanFiles()
                // when the useDisk on factory is false, just safe release them here
                safeRelease(body);
                throw new HttpRuntimeException(e);
            }
        } else {
            log.debug("Send HTTP request async: {}", req);
            channel.writeAndFlush(req);
        }
    }

    private static void safeRelease(MultipartBody body) {
        if (body.getToDeleteDataList() != null) {
            for (var httpData : body.getToDeleteDataList()) {
                ReferenceCountUtil.safeRelease(httpData);
            }
        }
    }

    /**
     * The abstract implementation of {@link HttpClient.Builder}.
     *
     * @author MJ Fang
     * @since 1.0
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
        protected CharSequence defaultUserAgent = DEFAULT_USER_AGENT_VALUE;

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
         * Returns the connection timeout seconds for this client.
         *
         * @return the connection timeout
         * @since 2.5
         */
        public int connectionTimeoutSeconds() {
            return (int) connectionTimeout().getSeconds();
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
         * @since 1.2
         */
        public ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory() {
            return this.proxyHandlerFactory;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self defaultUserAgent(CharSequence userAgent) {
            this.defaultUserAgent = userAgent;
            return (Self) this;
        }

        /**
         * Returns the default {@code user-agent} value.
         *
         * @return the default {@code user-agent} value, may be {@code null}
         * @since 3.8
         */
        public CharSequence defaultUserAgent() {
            return this.defaultUserAgent;
        }

    }

}
