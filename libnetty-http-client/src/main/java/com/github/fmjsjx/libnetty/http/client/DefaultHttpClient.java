package com.github.fmjsjx.libnetty.http.client;

import static java.net.InetSocketAddress.createUnresolved;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;

import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libcommon.util.pool.BlockingCachedPool;
import com.github.fmjsjx.libcommon.util.pool.CachedPool;
import com.github.fmjsjx.libcommon.util.pool.ConcurrentCachedPool;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.transport.TransportLibrary;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

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
                      ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory) {
        super(group, channelClass, sslContextProvider, compressionEnabled, proxyHandlerFactory, defaultRequestTimeout);
        this.shutdownGroupOnClose = shutdownGroupOnClose;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.maxCachedSizeEachDomain = maxCachedSizeEachDomain;
        this.cachedPoolFactory = cachedPoolFactory;
    }

    /**
     * Returns the maximum cached connection size.
     * 
     * @return always {@code 0}
     * @deprecated This method is deprecated and always returns {@code 0}.
     */
    @Deprecated
    public int maxCachedSize() {
        return 0;
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
        URI uri = request.uri();
        boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        boolean defaultPort = uri.getPort() == -1;
        int port = defaultPort ? (ssl ? 443 : 80) : uri.getPort();
        String host = uri.getHost();
        CompletableFuture<Response<T>> future = new CompletableFuture<>();
        RequestContext<T> requestContext = new RequestContext<>(request, future, contentHandler, executor);
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
                                            pipeline.addLast(
                                                    sslContextProvider.get().newHandler(ctx.alloc(), host, port));
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
            pipeline.addLast(new HttpContentDecompressor());
        }
        pipeline.addLast(new ChunkedWriteHandler());
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
        private final Optional<Executor> executor;

        private RequestContext(Request request, CompletableFuture<? super Response<T>> future,
                HttpContentHandler<T> contentHandler, Optional<Executor> executor) {
            this.request = request;
            this.future = future;
            this.contentHandler = contentHandler;
            this.executor = executor;
        }

        private void complete(FullHttpResponse msg) {
            complete(msg.protocolVersion(), msg.status(), msg.headers(), msg.content());
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

        private RequestContext<?> requestContext;

        private InternalHttpClientHandler(InetSocketAddress address, CharSequence headerHost,
                CachedPool<HttpConnection> cachedPool) {
            this.address = address;
            this.headerHost = headerHost;
            this.cachedPool = cachedPool;
        }

        private InternalHttpClientHandler(InetSocketAddress address, CharSequence headerHost,
                CachedPool<HttpConnection> cachedPool, Channel channel) {
            this.address = address;
            this.headerHost = headerHost;
            this.cachedPool = cachedPool;
            this.channel = channel;
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            this.channel = ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
            if (this.requestContext != null) {
                RequestContext<?> requestContext = this.requestContext;
                this.requestContext = null;
                if (!requestContext.future.isDone()) {
                    requestContext.future.completeExceptionally(cause);
                }
            } else {
                // remove HttpConnection from cache pool
                cachedPool.tryRelease(this);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                if (((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
                    ctx.close();
                    if (this.requestContext != null) {
                        RequestContext<?> requestContext = this.requestContext;
                        this.requestContext = null;
                        if (!requestContext.future.isDone()) {
                            requestContext.future.completeExceptionally(new TimeoutException());
                        }
                    } else {
                        // remove HttpConnection from cache pool
                        cachedPool.tryRelease(this);
                    }
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            if (this.requestContext != null) {
                RequestContext<?> requestContext = this.requestContext;
                this.requestContext = null;
                if (isOpen() && HttpUtil.isKeepAlive(msg)) {
                    if (!cachedPool.tryBack(this)) {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
                if (requestContext.executor.isPresent()) {
                    msg.retain();
                    requestContext.executor.get().execute(() -> {
                        try {
                            requestContext.complete(msg);
                        } finally {
                            msg.release();
                        }
                    });
                } else {
                    requestContext.complete(msg);
                }
            } else {
                // WARN: should not reach this line.
                // To be on the safe side, always close channel and remove it from cached pool.
                ctx.close();
                cachedPool.tryRelease(this);
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
                        this.requestContext = requestContext;
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
         * Sets the number of maximum cached connections size.
         * <p>
         * The default value is {@code 256}.
         * <p>
         * The minimum value is {@code 16}.
         * 
         * @param maxCachedSize the number of maximum cached connections size
         * @return this builder
         * 
         * @since 2.0
         * @deprecated Please always use {@link #maxCachedSizeEachDomain(int)}.
         */
        @Deprecated
        public Builder maxCachedSize(int maxCachedSize) {
            return this;
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
            TransportLibrary transportLibrary = TransportLibrary.getDefault();
            ThreadFactory threadFactory = new DefaultThreadFactory(DefaultHttpClient.class, true);
            return new DefaultHttpClient(transportLibrary.createIoGroup(ioThreads(), threadFactory),
                    transportLibrary.channelClass(), sslContextProvider(), compressionEnabled(), true,
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), maxCachedSizeEachDomain,
                    cachedPoolFactory, proxyHandlerFactory());
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
                    cachedPoolFactory, proxyHandlerFactory());
        }

    }

}
