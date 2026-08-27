package com.github.fmjsjx.libnetty.http.client;

import com.github.fmjsjx.libcommon.util.pool.BlockingCachedPool;
import com.github.fmjsjx.libcommon.util.pool.CachedPool;
import com.github.fmjsjx.libcommon.util.pool.ConcurrentCachedPool;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.transport.io.IoTransportLibrary;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.IntFunction;

import static java.net.InetSocketAddress.createUnresolved;

/**
 * The default implementation of {@link HttpClient} which will cache {@code TCP}
 * connections.
 *
 * @since 1.1
 *
 * @author MJ Fang
 *
 * @see AbstractHttpClient
 * @see SimpleHttpClient
 */
public class DefaultHttpClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpClient.class);

    private final boolean shutdownGroupOnClose;
    private final int connectionTimeoutSeconds;
    private final int maxContentLength;
    private final int maxCachedSizeEachDomain;
    private final IntFunction<CachedPool<HttpConnection>> cachedPoolFactory;

    private final ConcurrentMap<String, CachedPool<HttpConnection>> cachedPools = new ConcurrentHashMap<>();

    DefaultHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass,
                      SslContextProvider sslContextProvider, boolean compressionEnabled, boolean shutdownGroupOnClose,
                      int connectionTimeoutSeconds, Duration defaultRequestTimeout, int maxContentLength,
                      int maxCachedSizeEachDomain, IntFunction<CachedPool<HttpConnection>> cachedPoolFactory,
                      ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory, CharSequence defaultUserAgent) {
        super(group, channelClass, sslContextProvider, compressionEnabled, proxyHandlerFactory, defaultRequestTimeout, defaultUserAgent);
        this.shutdownGroupOnClose = shutdownGroupOnClose;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.maxCachedSizeEachDomain = maxCachedSizeEachDomain;
        this.cachedPoolFactory = cachedPoolFactory;
    }

    /**
     * Returns the maximum cached connection size for each domain.
     *
     * @return the maximum cached connection size for each domain
     * @since 2.1
     */
    public int maxCachedSizeEachDomain() {
        return maxCachedSizeEachDomain;
    }

    @Override
    protected void close0() {
        if (shutdownGroupOnClose) {
            log.debug("Shutdown {}", group);
            group.shutdownGracefully();
        }
        // clear all cached Pools
        cachedPools.values().forEach(CachedPool::clear);
    }

    @Override
    protected <T> CompletableFuture<Response<T>> sendAsync0(Request request, HttpContentHandler<T> contentHandler,
            Optional<Executor> executor) {
        var uri = request.uri();
        boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        boolean defaultPort = uri.getPort() == -1;
        int port = defaultPort ? (ssl ? 443 : 80) : uri.getPort();
        String host = uri.getHost();
        CompletableFuture<Response<T>> future = new CompletableFuture<>();
        RequestContext<T> requestContext = new RequestContext<>(request, future, contentHandler, executor.orElse(null));
        String addressKey = host + ":" + port;
        var cachedPool = getCachedConnectionPool(addressKey);
        Optional<HttpConnection> conn = tryPollOne(cachedPool);
        if (conn.isPresent()) {
            conn.get().sendAsnyc(requestContext);
        } else {
            String headerHost = defaultPort ? host : host + ":" + port;
            if (proxyHandlerFactory.isPresent()) {
                ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory = this.proxyHandlerFactory.get();
                Bootstrap b = new Bootstrap().resolver(NoopAddressResolverGroup.INSTANCE).group(group)
                        .channel(channelClass).option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline cp = ch.pipeline();
                                cp.addLast(proxyHandlerFactory.create());
                                cp.addLast(new ProxyEventHandler((ctx, obj) -> {
                                    if (obj instanceof Throwable) {
                                        future.completeExceptionally((Throwable) obj);
                                    } else if (obj instanceof ProxyConnectionEvent) {
                                        ChannelPipeline pipeline = ctx.pipeline();
                                        var handler = new InternalHttpClientHandler(createUnresolved(host, port),
                                                headerHost, cachedPool, ctx.channel());
                                        pipeline.addLast(new IdleStateHandler(0, 0, connectionTimeoutSeconds));
                                        if (ssl) {
                                            pipeline.addLast(sslContextProvider.get().newHandler(ctx.alloc(), host, port));
                                        }
                                        addHttpHandlers(pipeline, handler);
                                        handler.sendAsnyc(requestContext);
                                    } else {
                                        future.completeExceptionally(
                                                new HttpRuntimeException("unknown event type " + obj.getClass()));
                                    }
                                }));
                            }
                        });
                b.connect(host, port).addListener((ChannelFuture cf) -> {
                    if (!cf.isSuccess()) {
                        future.completeExceptionally(cf.cause());
                    }
                });
            } else {
                var handler = new InternalHttpClientHandler(createUnresolved(host, port), headerHost, cachedPool);
                Bootstrap b = new Bootstrap().group(group).channel(channelClass).option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline cp = ch.pipeline();
                                cp.addLast(new IdleStateHandler(0, 0, connectionTimeoutSeconds));
                                if (ssl) {
                                    cp.addLast(sslContextProvider.get().newHandler(ch.alloc(), host, port));
                                }
                                addHttpHandlers(cp, handler);
                            }
                        });
                b.connect(handler.address()).addListener((ChannelFuture cf) -> {
                    if (cf.isSuccess()) {
                        handler.sendAsnyc(requestContext);
                    } else {
                        future.completeExceptionally(cf.cause());
                    }
                });
            }
        }
        return future;
    }

    private void addHttpHandlers(ChannelPipeline pipeline, InternalHttpClientHandler handler) {
        pipeline.addLast(new HttpClientCodec());
        if (autoDecompression) {
            pipeline.addLast(new HttpContentDecompressor(0));
        }
        pipeline.addLast(new ChunkedWriteHandler());
        // the interceptor bypasses the HttpObjectAggregator for the streaming
        // requests while a fresh aggregator instance is used for each
        // aggregated request
        pipeline.addLast(handler.chunkedContentInterceptor);
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(handler);
    }

    private CachedPool<HttpConnection> getCachedConnectionPool(String addressKey) {
        return cachedPools.computeIfAbsent(addressKey, k -> cachedPoolFactory.apply(maxCachedSizeEachDomain));
    }

    private Optional<HttpConnection> tryPollOne(CachedPool<HttpConnection> cachedPool) {
        for (;;) {
            var o = cachedPool.tryTake();
            if (o.isEmpty()) {
                return o;
            } else if (o.get().isActive()) {
                return o;
            }
        }
    }

    private static final class RequestContext<T> {

        private final Request request;
        private final CompletableFuture<? super Response<T>> future;
        private final HttpContentHandler<T> contentHandler;
        private final Executor executor;
        private final ChunkedHttpContentHandler<T> chunkedContentHandler;
        // whether onComplete or onError has been invoked (or scheduled)
        // on the chunked content handler, i.e. the handler has reached its final state
        private boolean contentCompleted;

        private RequestContext(Request request, CompletableFuture<? super Response<T>> future,
                HttpContentHandler<T> contentHandler, Executor executor) {
            this.request = request;
            this.future = future;
            this.contentHandler = contentHandler;
            this.executor = executor;
            if (contentHandler instanceof ChunkedHttpContentHandler<T> chunkedHttpContentHandler) {
                chunkedContentHandler = chunkedHttpContentHandler;
            } else {
                chunkedContentHandler = null;
            }
        }

        private void responseComplete(HttpResponse msg) {
            if (msg instanceof FullHttpResponse fullResponse) {
                complete(fullResponse);
            } else {
                if (chunkedContentHandler != null) {
                    var response = new DefaultResponse<>(msg.protocolVersion(), msg.status(), msg.headers(),
                            chunkedContentHandler.get());
                    var executor = this.executor;
                    if (executor != null) {
                        executor.execute(() -> future.complete(response));
                    } else {
                        future.complete(response);
                    }
                } else {
                    // Not Full HttpResponse only supported with ChunkedHttpContentHandler
                    future.completeExceptionally(new IllegalStateException("Invalid content handler type " + contentHandler.getClass() +
                            ", expected a ChunkedHttpContentHandler"));
                }
            }
        }

        /**
         * Handle the chunk and returns if the chunk is the last chunk.
         *
         * @param chunk the chunk to handle
         * @return true if the chunk is the last chunk, false otherwise
         */
        private boolean handleChunk(HttpContent chunk) {
            var chunkedContentHandler = this.chunkedContentHandler;
            assert chunkedContentHandler != null;
            var executor = this.executor;
            if (chunk instanceof LastHttpContent lastHttpContent) {
                contentCompleted = true;
                if (executor != null) {
                    lastHttpContent.retain();
                    executor.execute(() -> {
                        try {
                            if (lastHttpContent.content().isReadable()) {
                                chunkedContentHandler.accept(lastHttpContent.content());
                            }
                            chunkedContentHandler.onComplete();
                        } finally {
                            lastHttpContent.release();
                        }
                    });
                } else {
                    if (lastHttpContent.content().isReadable()) {
                        chunkedContentHandler.accept(lastHttpContent.content());
                    }
                    chunkedContentHandler.onComplete();
                }
                return true;
            }
            if (executor != null) {
                chunk.retain();
                executor.execute(() -> {
                    try {
                        chunkedContentHandler.accept(chunk.content());
                    } finally {
                        chunk.release();
                    }
                });
            } else {
                chunkedContentHandler.accept(chunk.content());
            }
            return false;
        }

        /**
         * Handle the error occurs on the request.
         *
         * @param cause the cause
         */
        private void onError(Throwable cause) {
            if (future.isDone()) {
                // the response head has been received and the content is streaming,
                // notify the chunked content handler (at most once)
                var chunkedContentHandler = this.chunkedContentHandler;
                if (chunkedContentHandler != null && !contentCompleted) {
                    contentCompleted = true;
                    var executor = this.executor;
                    if (executor != null) {
                        executor.execute(() -> chunkedContentHandler.onError(cause));
                    } else {
                        chunkedContentHandler.onError(cause);
                    }
                }
            } else {
                future.completeExceptionally(cause);
            }
        }

        private void complete(FullHttpResponse msg) {
            var executor = this.executor;
            if (executor != null) {
                msg.retain();
                executor.execute(() -> {
                    try {
                        complete(msg.protocolVersion(), msg.status(), msg.headers(), msg.content());
                    } finally {
                        msg.release();
                    }
                });
            } else {
                complete(msg.protocolVersion(), msg.status(), msg.headers(), msg.content());
            }
        }

        private void complete(HttpVersion version, HttpResponseStatus status, HttpHeaders headers, ByteBuf content) {
            DefaultResponse<T> response = new DefaultResponse<>(version, status, headers,
                    contentHandler.apply(content));
            future.complete(response);
        }

    }

    private interface HttpConnection {

        Channel channel();

        default boolean isActive() {
            Channel channel = channel();
            return channel != null && channel.isActive();
        }

        void sendAsnyc(RequestContext<?> requestContext);

    }

    private final class InternalHttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse>
            implements HttpConnection {

        private final InetSocketAddress address;
        private final CharSequence headerHost;
        private final CachedPool<HttpConnection> cachedPool;
        private volatile Channel channel;

        private final ChunkedContentInterceptor chunkedContentInterceptor;

        private RequestContext<?> requestContext;

        private InternalHttpClientHandler(InetSocketAddress address, CharSequence headerHost,
                CachedPool<HttpConnection> cachedPool) {
            this.address = address;
            this.headerHost = headerHost;
            this.cachedPool = cachedPool;
            this.chunkedContentInterceptor = new ChunkedContentInterceptor(this);
        }

        private InternalHttpClientHandler(InetSocketAddress address, CharSequence headerHost,
                CachedPool<HttpConnection> cachedPool, Channel channel) {
            this.address = address;
            this.headerHost = headerHost;
            this.cachedPool = cachedPool;
            this.channel = channel;
            this.chunkedContentInterceptor = new ChunkedContentInterceptor(this);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            this.channel = ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.debug("Error occurs on default client channel: {}", ctx.channel(), cause);
            try {
                onErrorCaught(cause);
            } finally {
                ctx.close();
            }
        }

        private void onErrorCaught(Throwable cause) {
            RequestContext<?> requestContext = this.requestContext;
            if (requestContext != null) {
                this.requestContext = null;
                requestContext.onError(cause);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            try {
                RequestContext<?> requestContext = this.requestContext;
                if (requestContext != null) {
                    // an in-flight aggregated request is present and the connection
                    // is closed before the request completed, fail the request
                    this.requestContext = null;
                    requestContext.onError(new ClosedChannelException());
                }
            } finally {
                // remove HttpConnection from cache pool when channel inactive
                cachedPool.tryRelease(this);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                if (((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
                    try {
                        onErrorCaught(new TimeoutException());
                    } finally {
                        ctx.close();
                    }
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            RequestContext<?> requestContext = this.requestContext;
            if (requestContext == null) {
                // WARN: should not reach this line.
                // To be on the safe side, always close channel and remove it from cached pool.
                ctx.close();
                return;
            }
            this.requestContext = null;
            releaseConnection(ctx, HttpUtil.isKeepAlive(msg));
            requestContext.responseComplete(msg);
        }

        /**
         * Release the connection after a request completed, back to the cached
         * pool if keep-alive is enabled, or close the channel otherwise.
         *
         * @param ctx       the {@link ChannelHandlerContext}
         * @param keepAlive whether the connection should be kept alive
         */
        private void releaseConnection(ChannelHandlerContext ctx, boolean keepAlive) {
            if (isOpen() && keepAlive) {
                if (!cachedPool.tryBack(this)) {
                    ctx.close();
                }
            } else {
                ctx.close();
            }
        }

        public InetSocketAddress address() {
            return address;
        }

        @Override
        public Channel channel() {
            return channel;
        }

        @Override
        public void sendAsnyc(RequestContext<?> requestContext) {
            if (channel.isActive()) {
                channel.eventLoop().execute(() -> {
                    Request request = requestContext.request;
                    if (channel.isActive()) {
                        if (requestContext.chunkedContentHandler != null) {
                            // streaming mode, the interceptor handles the response
                            chunkedContentInterceptor.requestContext = requestContext;
                        } else {
                            // aggregated mode, use a fresh HttpObjectAggregator
                            // for each request as the instance can not be reused
                            this.requestContext = requestContext;
                        }
                        URI uri = request.uri();
                        String path = uri.getRawPath();
                        String query = uri.getRawQuery();
                        String requestUri = query == null ? path : path + "?" + query;
                        var req = createHttpRequest(request, requestUri);
                        sendHttpRequest(req, channel, request);
                    } else {
                        requestContext.future.completeExceptionally(new ClosedChannelException());
                    }
                });
            } else {
                requestContext.future.completeExceptionally(new ClosedChannelException());
            }
        }

        private HttpRequest createHttpRequest(Request request, String requestUri) {
            return DefaultHttpClient.this.createHttpRequest(channel.alloc(), request, headerHost, requestUri, true);
        }

    }

    /**
     * A pipeline handler which intercepts the streaming (non-aggregated) HTTP
     * responses for the requests using a {@link ChunkedHttpContentHandler}.
     * <p>
     * When the {@code requestContext} is present (streaming mode), the
     * {@link HttpResponse} and {@link HttpContent} messages are intercepted
     * and handled directly without being propagated to the
     * {@link HttpObjectAggregator} and the {@link InternalHttpClientHandler};
     * when the {@code requestContext} is absent (aggregated mode), all
     * messages are passed through.
     */
    private final class ChunkedContentInterceptor extends ChannelInboundHandlerAdapter {

        private final InternalHttpClientHandler clientHandler;

        private RequestContext<?> requestContext;
        // whether the response head of the streaming response
        // in progress indicates keep-alive
        private boolean keepAlive;

        private ChunkedContentInterceptor(InternalHttpClientHandler clientHandler) {
            this.clientHandler = clientHandler;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            RequestContext<?> requestContext = this.requestContext;
            if (requestContext == null) {
                // aggregated mode, pass through to the HttpObjectAggregator
                ctx.fireChannelRead(msg);
                return;
            }
            // streaming mode, intercept and handle the message directly,
            // do NOT propagate it so that the HttpObjectAggregator and the
            // InternalHttpClientHandler are bypassed
            if (msg instanceof HttpResponse httpResponse) {
                // complete the future immediately on the response head received
                // so that the caller is able to access the status and headers
                // as soon as possible while the content is streamed afterward
                keepAlive = HttpUtil.isKeepAlive(httpResponse);
                requestContext.responseComplete(httpResponse);
                ReferenceCountUtil.release(msg);
            } else if (msg instanceof HttpContent httpContent) {
                try {
                    if (requestContext.handleChunk(httpContent)) {
                        // last chunk received, request completed
                        this.requestContext = null;
                        clientHandler.releaseConnection(ctx, keepAlive);
                        keepAlive = false;
                    }
                } finally {
                    httpContent.release();
                }
            } else {
                // unexpected message type, release and close for safety
                ReferenceCountUtil.release(msg);
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            RequestContext<?> requestContext = this.requestContext;
            if (requestContext == null) {
                // aggregated mode, propagate to the next handler
                ctx.fireExceptionCaught(cause);
                return;
            }
            log.debug("Error occurs on receiving chunked content channel: {}", ctx.channel(), cause);
            try {
                this.requestContext = null;
                requestContext.onError(cause);
            } finally {
                ctx.close();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            try {
                RequestContext<?> requestContext = this.requestContext;
                if (requestContext != null) {
                    // a streaming request is in-flight and the connection is
                    // closed before the request completed, fail the request
                    this.requestContext = null;
                    requestContext.onError(new ClosedChannelException());
                }
            } finally {
                ctx.fireChannelInactive();
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            RequestContext<?> requestContext = this.requestContext;
            if (requestContext != null && evt instanceof IdleStateEvent
                    && ((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
                log.debug("Idle timeout on receiving chunked content channel: {}", ctx.channel());
                try {
                    this.requestContext = null;
                    requestContext.onError(new TimeoutException());
                } finally {
                    ctx.close();
                }
                return;
            }
            ctx.fireUserEventTriggered(evt);
        }

    }

    /**
     * Returns a new {@link Builder} with default settings.
     *
     * @return a {@code Builder}.
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link DefaultHttpClient} with default settings.
     *
     * @return a {@code ConnectionCachedHttpClient}
     */
    public static final DefaultHttpClient build() {
        return builder().build();
    }

    /**
     * Builder of {@link DefaultHttpClient}.
     *
     * @since 1.0
     *
     * @author MJ Fang
     */
    public static final class Builder extends AbstractBuilder<DefaultHttpClient, Builder> {

        private int maxCachedSizeEachDomain = 16;
        private IntFunction<CachedPool<HttpConnection>> cachedPoolFactory = ConcurrentCachedPool::new;

        private Builder() {
        }

        /**
         * Sets the number of maximum cached connections size for each domain.
         * <p>
         * The default value is {@code 16}.
         * <p>
         * The minimum value is {@code 1}.
         *
         * @param maxCachedSize the number of maximum cached connections size for each
         *                      domain
         * @return this builder
         *
         * @since 2.1
         */
        public Builder maxCachedSizeEachDomain(int maxCachedSize) {
            this.maxCachedSizeEachDomain = Math.max(1, maxCachedSize);
            return this;
        }

        /**
         * Set the factory of cached pool.
         * <p>
         * The default factory is {@code ConcurrentCachedPool::new}.
         *
         * @param cachedPoolFactory the factory of cached pool
         * @return this builder
         *
         * @since 2.1
         */
        Builder cachedPoolFactory(IntFunction<CachedPool<HttpConnection>> cachedPoolFactory) {
            this.cachedPoolFactory = Objects.requireNonNull(cachedPoolFactory, "cachedPoolFactory must not be null");
            return this;
        }

        /**
         * Use {@link BlockingCachedPool} instead of default
         * {@link ConcurrentCachedPool}.
         *
         * @return this builder
         */
        public Builder useBlockingCachedPool() {
            return cachedPoolFactory(BlockingCachedPool::new);
        }

        /**
         * Returns a new {@link DefaultHttpClient} built from the current state of this
         * builder with internal {@link EventLoopGroup}.
         *
         * @return a new {@code ConnectionCachedHttpClient}
         */
        @Override
        public DefaultHttpClient build() {
            ensureSslContext();
            IoTransportLibrary transportLibrary = IoTransportLibrary.getDefault();
            ThreadFactory threadFactory = new DefaultThreadFactory(DefaultHttpClient.class, true);
            return new DefaultHttpClient(transportLibrary.createGroup(ioThreads(), threadFactory),
                    transportLibrary.channelClass(), sslContextProvider(), compressionEnabled(), true,
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), maxCachedSizeEachDomain,
                    cachedPoolFactory, proxyHandlerFactory(), defaultUserAgent());
        }

        /**
         * Returns a new {@link DefaultHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
         * <p>
         * In this solution, the builder option {@code ioThreads} will be ignored
         *
         * @param group the {@link EventLoopGroup}
         * @return a new {@code ConnectionCachedHttpClient}
         */
        public DefaultHttpClient build(EventLoopGroup group) {
            Class<? extends Channel> channelClass = SocketChannelUtil.fromEventLoopGroup(group);
            return build(group, channelClass);
        }

        /**
         * Returns a new {@link DefaultHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
         * <p>
         * In this solution, the builder option {@code ioThreads} will be ignored
         *
         * @param group        the {@link EventLoopGroup}
         * @param channelClass the {@link Class} of {@link Channel}
         * @return a new {@code ConnectionCachedHttpClient}
         */
        public DefaultHttpClient build(EventLoopGroup group, Class<? extends Channel> channelClass) {
            ensureSslContext();
            return new DefaultHttpClient(group, channelClass, sslContextProvider(), compressionEnabled(), false,
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), maxCachedSizeEachDomain,
                    cachedPoolFactory, proxyHandlerFactory(), defaultUserAgent());
        }

    }

}
