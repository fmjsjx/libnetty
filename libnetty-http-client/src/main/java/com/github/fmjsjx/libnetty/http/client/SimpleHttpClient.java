package com.github.fmjsjx.libnetty.http.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.transport.TransportLibrary;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Simple implementation of {@link HttpClient} uses short connections (create
 * and close channel for each request).
 *
 * @author MJ Fang
 * @see AbstractHttpClient
 * @see DefaultHttpClient
 * @since 1.0
 */
public class SimpleHttpClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpClient.class);

    /**
     * Builder of {@link SimpleHttpClient}.
     *
     * @author MJ Fang
     * @since 1.0
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder extends AbstractBuilder<SimpleHttpClient, Builder> {

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with internal {@link EventLoopGroup}.
         *
         * @return a new {@code SimpleHttpClient}
         */
        @Override
        public SimpleHttpClient build() {
            ensureSslContext();
            TransportLibrary transportLibrary = TransportLibrary.getDefault();
            ThreadFactory threadFactory = new DefaultThreadFactory(SimpleHttpClient.class, true);
            return new SimpleHttpClient(transportLibrary.createGroup(ioThreads(), threadFactory),
                    transportLibrary.channelClass(), sslContextProvider(), compressionEnabled(), true,
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), proxyHandlerFactory());
        }

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
         * <p>
         * In this solution, the builder option {@code ioThreads} will be ignored
         *
         * @param group the {@link EventLoopGroup}
         * @return a new {@code SimpleHttpClient}
         */
        public SimpleHttpClient build(EventLoopGroup group) {
            Class<? extends Channel> channelClass = SocketChannelUtil.fromEventLoopGroup(group);
            return build(group, channelClass);
        }

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
         * <p>
         * In this solution, the builder option {@code ioThreads} will be ignored
         *
         * @param group        the {@link EventLoopGroup}
         * @param channelClass the {@link Class} of {@link Channel}
         * @return a new {@code SimpleHttpClient}
         */
        public SimpleHttpClient build(EventLoopGroup group, Class<? extends Channel> channelClass) {
            ensureSslContext();
            return new SimpleHttpClient(group, channelClass, sslContextProvider(), compressionEnabled(), false,
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), proxyHandlerFactory());
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
     * Returns a new {@link SimpleHttpClient} with default settings.
     *
     * @return a {@code SimpleHttpClient}
     */
    public static final SimpleHttpClient build() {
        return builder().build();
    }

    private final boolean shutdownGroupOnClose;
    private final int connectionTimeoutSeconds;
    private final int maxContentLength;

    SimpleHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContextProvider sslContextProvider,
                     boolean compressionEnabled, boolean shutdownGroupOnClose, int connectionTimeoutSeconds,
                     Duration defaultRequestTimeout, int maxContentLength,
                     ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory) {
        super(group, channelClass, sslContextProvider, compressionEnabled, proxyHandlerFactory, defaultRequestTimeout);
        this.shutdownGroupOnClose = shutdownGroupOnClose;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void close0() {
        if (shutdownGroupOnClose) {
            log.debug("Shutdown {}", group);
            group.shutdownGracefully();
        }
    }

    @Override
    protected <T> CompletableFuture<Response<T>> sendAsync0(Request request, HttpContentHandler<T> contentHandler,
                                                            Optional<Executor> executor) {
        URI uri = request.uri();
        boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        boolean defaultPort = uri.getPort() == -1;
        int port = defaultPort ? (ssl ? 443 : 80) : uri.getPort();
        String host = uri.getHost();
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        String requestUri = query == null ? path : path + "?" + query;
        CompletableFuture<Response<T>> future = new CompletableFuture<>();
        Bootstrap b = new Bootstrap().group(group).channel(channelClass).option(ChannelOption.TCP_NODELAY, true);
        if (proxyHandlerFactory.isPresent()) {
            b.resolver(NoopAddressResolverGroup.INSTANCE);
            ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory = this.proxyHandlerFactory.get();
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline cp = ch.pipeline();
                    cp.addLast(proxyHandlerFactory.create());
                    cp.addLast(new ProxyEventHandler((ctx, obj) -> {
                        if (obj instanceof Throwable) {
                            future.completeExceptionally((Throwable) obj);
                        } else if (obj instanceof ProxyConnectionEvent) {
                            var pipeline = ctx.pipeline();
                            pipeline.addLast(new ReadTimeoutHandler(connectionTimeoutSeconds));
                            if (ssl) {
                                pipeline.addLast(sslContextProvider.get().newHandler(ctx.alloc(), host, port));
                            }
                            addHttpHandlers(pipeline, future, contentHandler, executor);
                            var req = createHttpRequest(ctx.alloc(), request, defaultPort, port, host, requestUri);
                            sendHttpRequest(req, ctx.channel(), request);
                        } else {
                            future.completeExceptionally(
                                    new HttpRuntimeException("unknown event type " + obj.getClass()));
                        }
                    }));
                }
            });
            b.connect(address).addListener((ChannelFuture cf) -> {
                if (!cf.isSuccess()) {
                    future.completeExceptionally(cf.cause());
                }
            });
        } else {
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline cp = ch.pipeline();
                    cp.addLast(new ReadTimeoutHandler(connectionTimeoutSeconds));
                    if (ssl) {
                        cp.addLast(sslContextProvider.get().newHandler(ch.alloc(), host, port));
                    }
                    addHttpHandlers(cp, future, contentHandler, executor);
                }
            });
            b.connect(address).addListener((ChannelFuture cf) -> {
                if (cf.isSuccess()) {
                    var req = createHttpRequest(cf.channel().alloc(), request, defaultPort, port,
                            host, requestUri);
                    sendHttpRequest(req, cf.channel(), request);
                } else {
                    future.completeExceptionally(cf.cause());
                }
            });
        }
        return future;
    }

    private <T> void addHttpHandlers(ChannelPipeline pipeline, CompletableFuture<Response<T>> future,
                                     HttpContentHandler<T> contentHandler, Optional<Executor> executor) {
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new SimpleHttpClientHandler<>(future, contentHandler, executor));
    }

    private HttpRequest createHttpRequest(ByteBufAllocator alloc, Request request, boolean defaultPort,
                                          int port, String host, String requestUri) {
        var headerHost = defaultPort ? host : host + ":" + port;
        return createHttpRequest(alloc, request, headerHost, requestUri);
    }

    private static final class SimpleHttpClientHandler<T> extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CompletableFuture<Response<T>> future;
        private final HttpContentHandler<T> contentHandler;
        private final Optional<Executor> executor;

        private SimpleHttpClientHandler(CompletableFuture<Response<T>> future, HttpContentHandler<T> contentHandler,
                                        Optional<Executor> executor) {
            this.future = future;
            this.contentHandler = contentHandler;
            this.executor = executor;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.debug("Error occurs", cause);
            if (!future.isDone()) {
                future.completeExceptionally(cause);
            }
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!future.isDone()) {
                future.completeExceptionally(new IllegalStateException("No Response Content"));
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            if (executor.isPresent()) {
                msg.retain();
                executor.get().execute(() -> {
                    try {
                        future.complete(buildResponse(msg));
                    } finally {
                        msg.release();
                    }
                });
            } else {
                future.complete(buildResponse(msg));
            }
            ctx.close();
        }

        private Response<T> buildResponse(FullHttpResponse msg) {
            return new DefaultResponse<>(msg.protocolVersion(), msg.status(), msg.headers(),
                    contentHandler.apply(msg.content()));
        }

    }

}
