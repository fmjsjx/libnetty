package com.github.fmjsjx.libnetty.http.client;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.transport.io.IoTransportLibrary;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of {@link HttpClient} uses short connections (create
 * and close channel for each request).
 *
 * @author MJ Fang
 * @see AbstractHttpClient
 * @see DefaultHttpClient
 * @since 1.0
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SimpleHttpClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpClient.class);

    /**
     * Builder of {@link SimpleHttpClient}.
     *
     * @author MJ Fang
     * @since 1.0
     */
    public static final class Builder extends AbstractBuilder<SimpleHttpClient, Builder> {

        private Builder() {}

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with internal {@link EventLoopGroup}.
         *
         * @return a new {@code SimpleHttpClient}
         */
        @Override
        public SimpleHttpClient build() {
            ensureSslContext();
            IoTransportLibrary transportLibrary = IoTransportLibrary.getDefault();
            ThreadFactory threadFactory = new DefaultThreadFactory(SimpleHttpClient.class, true);
            return new SimpleHttpClient(transportLibrary.createGroup(ioThreads(), threadFactory),
                    transportLibrary.channelClass(), sslContextProvider(), compressionEnabled(), true,
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), proxyHandlerFactory(),
                    defaultUserAgent());
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
                    connectionTimeoutSeconds(), requestTimeout(), maxContentLength(), proxyHandlerFactory(),
                    defaultUserAgent());
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
                     ProxyHandlerFactory<? extends ProxyHandler> proxyHandlerFactory, CharSequence defaultUserAgent) {
        super(group, channelClass, sslContextProvider, compressionEnabled, proxyHandlerFactory, defaultRequestTimeout, defaultUserAgent);
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
        var uri = request.uri();
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
        if (autoDecompression) {
            pipeline.addLast(new HttpContentDecompressor(0));
        }
        pipeline.addLast(new ChunkedWriteHandler());
        if (contentHandler instanceof ChunkedHttpContentHandler<T> chunkedHttpContentHandler) {
            pipeline.addLast(new ChunkedContentSimpleHttpClientHandler<>(future, chunkedHttpContentHandler, executor));
        } else {
            pipeline.addLast(new HttpObjectAggregator(maxContentLength));
            pipeline.addLast(new SimpleHttpClientHandler<>(future, contentHandler, executor));
        }
    }

    private HttpRequest createHttpRequest(ByteBufAllocator alloc, Request request, boolean defaultPort,
                                          int port, String host, String requestUri) {
        var headerHost = defaultPort ? host : host + ":" + port;
        return createHttpRequest(alloc, request, headerHost, requestUri, false);
    }

    private static final class SimpleHttpClientHandler<T> extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CompletableFuture<Response<T>> future;
        private final HttpContentHandler<T> contentHandler;
        private final Executor executor;

        private boolean anticipatedClosure;

        private SimpleHttpClientHandler(CompletableFuture<Response<T>> future, HttpContentHandler<T> contentHandler,
                                        Optional<Executor> executor) {
            this.future = future;
            this.contentHandler = contentHandler;
            this.executor = executor.orElse(null);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            try {
                log.debug("Error occurs on simple client channel: {}", ctx.channel(), cause);
                if (!future.isDone()) {
                    futureCompleteExceptionally(cause);
                }
            } finally {
                anticipatedClose(ctx);
            }
        }

        private void anticipatedClose(ChannelHandlerContext ctx) {
            anticipatedClosure = true;
            ctx.close();
        }

        private void futureCompleteExceptionally(Throwable cause) {
            if (executor != null) {
                executor.execute(() -> future.completeExceptionally(cause));
            } else {
                future.completeExceptionally(cause);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!anticipatedClosure) {
                if (!future.isDone()) {
                    futureCompleteExceptionally(new IllegalStateException("No Response Content"));
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            if (executor != null) {
                msg.retain();
                executor.execute(() -> {
                    try {
                        future.complete(buildResponse(msg));
                    } finally {
                        msg.release();
                    }
                });
            } else {
                future.complete(buildResponse(msg));
            }
            anticipatedClose(ctx);
        }

        private Response<T> buildResponse(FullHttpResponse msg) {
            return new DefaultResponse<>(msg.protocolVersion(), msg.status(), msg.headers(),
                    contentHandler.apply(msg.content()), msg.trailingHeaders());
        }

    }

    private static final class ChunkedContentSimpleHttpClientHandler<T>
            extends SimpleChannelInboundHandler<HttpObject> {

        private final CompletableFuture<Response<T>> future;
        private final ChunkedHttpContentHandler<T> contentHandler;
        private final Executor executor;

        // whether onComplete or onError has been invoked (or scheduled)
        // on the content handler, i.e. the handler has reached its final state
        private boolean contentCompleted;
        private Consumer<HttpHeaders> trailingHeadersSetter;
        private boolean anticipatedClosure;

        private ChunkedContentSimpleHttpClientHandler(
                CompletableFuture<Response<T>> future,
                ChunkedHttpContentHandler<T> contentHandler,
                Optional<Executor> executor) {
            this.future = future;
            this.contentHandler = contentHandler;
            this.executor = executor.orElse(null);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.debug("Error occurs on receiving chunked content channel: {}", ctx.channel(), cause);
            try {
                if (future.isDone()) {
                    onError0(cause);
                } else {
                    futureCompleteExceptionally(cause);
                }
            } finally {
                anticipatedClose(ctx);
            }
        }

        private void anticipatedClose(ChannelHandlerContext ctx) {
            anticipatedClosure = true;
            ctx.close();
        }

        private void futureCompleteExceptionally(Throwable cause) {
            if (executor != null) {
                executor.execute(() -> future.completeExceptionally(cause));
            } else {
                future.completeExceptionally(cause);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            contentHandler.onBind(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!anticipatedClosure) {
                if (future.isDone()) {
                    onError0(new IllegalStateException("Connection closed before the content completed"));
                } else {
                    futureCompleteExceptionally(new IllegalStateException("Connection closed before the content completed"));
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            if (msg instanceof HttpResponse httpResponse) {
                // complete the future immediately on the response head received
                // so that the caller is able to access the status and headers
                // as soon as possible while the content is streamed afterward
                Response<T> response = buildResponse(httpResponse);
                if (executor != null) {
                    executor.execute(() -> future.complete(response));
                } else {
                    future.complete(response);
                }
            }
            if (msg instanceof HttpContent httpContent) {
                if (httpContent instanceof LastHttpContent lastHttpContent) {
                    contentCompleted = true;
                    httpContent.retain();
                    if (lastHttpContent.content().isReadable()) {
                        contentHandler.accept(lastHttpContent.content());
                    }
                    trailingHeadersSetter.accept(lastHttpContent.trailingHeaders());
                    contentHandler.onComplete();
                    anticipatedClose(ctx);
                    return;
                }
                contentHandler.accept(httpContent.content());
            }
        }

        private void onError0(Throwable cause) {
            if (!contentCompleted) {
                contentCompleted = true;
                contentHandler.onError(cause);
            }
        }

        private Response<T> buildResponse(HttpResponse response) {
            var trailingHeadersRef = new AtomicReference<HttpHeaders>();
            trailingHeadersSetter = trailingHeadersRef::set;
            return new DefaultResponse<>(response.protocolVersion(), response.status(), response.headers(),
                    contentHandler.get(), trailingHeadersRef::get);
        }

    }

}
