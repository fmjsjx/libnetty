package com.github.fmjsjx.libnetty.http.client;

import static com.github.fmjsjx.libnetty.http.HttpUtil.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpMethod.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
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
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

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
    private final int timeoutSeconds;
    private final int maxContentLength;

    private final ConcurrentMap<String, ConcurrentLinkedDeque<HttpConnection>> cachedPools = new ConcurrentHashMap<String, ConcurrentLinkedDeque<HttpConnection>>();

    DefaultHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContextProvider sslContextProvider,
            boolean compressionEnabled, boolean shutdownGroupOnClose, int timeoutSeconds, int maxContentLength) {
        super(group, channelClass, sslContextProvider, compressionEnabled);
        this.shutdownGroupOnClose = shutdownGroupOnClose;
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void close0() {
        if (shutdownGroupOnClose) {
            log.debug("Shutdown {}", group);
            group.shutdownGracefully();
        }
        cachedPools.values().forEach(ConcurrentLinkedDeque::clear);
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
        ConcurrentLinkedDeque<HttpConnection> cachedPool = getCachedConnectionPool(addressKey);
        Optional<HttpConnection> conn = tryPollOne(cachedPool);
        if (conn.isPresent()) {
            conn.get().sendAsnyc(requestContext);
        } else {
            InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
            String headerHost = defaultPort ? host : host + ":" + port;
            InternalHttpClientHandler handler = new InternalHttpClientHandler(address, headerHost, cachedPool);
            Bootstrap b = new Bootstrap().group(group).channel(channelClass).option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline cp = ch.pipeline();
                            cp.addLast(new IdleStateHandler(0, 0, timeoutSeconds));
                            if (ssl) {
                                cp.addLast(sslContextProvider.get().newHandler(ch.alloc(), host, port));
                            }
                            cp.addLast(new HttpClientCodec());
                            cp.addLast(new HttpContentDecompressor());
                            cp.addLast(new HttpObjectAggregator(maxContentLength));
                            cp.addLast(handler);
                        }
                    });
            b.connect(address).addListener((ChannelFuture cf) -> {
                if (cf.isSuccess()) {
                    handler.sendAsnyc(requestContext);
                } else {
                    future.completeExceptionally(cf.cause());
                }
            });
        }
        return future;
    }

    private ConcurrentLinkedDeque<HttpConnection> getCachedConnectionPool(String addressKey) {
        return cachedPools.computeIfAbsent(addressKey, k -> new ConcurrentLinkedDeque<>());
    }

    private static final Optional<HttpConnection> tryPollOne(ConcurrentLinkedDeque<HttpConnection> cachedPool) {
        for (HttpConnection conn = cachedPool.pollLast(); conn != null; conn = cachedPool.pollLast()) {
            if (conn.isActive()) {
                return Optional.of(conn);
            }
        }
        return Optional.empty();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class RequestContext<T> {

        private final Request request;
        private final CompletableFuture<? super Response<T>> future;
        private final HttpContentHandler<T> contentHandler;
        private final Optional<Executor> executor;

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

        InetSocketAddress address();

        Channel channel();

        default boolean isActive() {
            Channel channel = channel();
            return channel != null && channel.isActive();
        }

        void sendAsnyc(RequestContext<?> requestContext);

    }

    @RequiredArgsConstructor
    private final class InternalHttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse>
            implements HttpConnection {

        private final InetSocketAddress address;
        private final CharSequence headerHost;
        private final ConcurrentLinkedDeque<HttpConnection> cachedPool;
        private volatile Channel channel;

        private RequestContext<?> requestContext;

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            this.channel = ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
            if (this.requestContext != null) {
                RequestContext<?> requestContext = this.requestContext;
                this.requestContext = null;
                if (!requestContext.future.isDone()) {
                    requestContext.future.completeExceptionally(cause);
                }
            } else {
                // remove HttpConnection from cache pool
                cachedPool.removeFirstOccurrence(this);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
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
                        cachedPool.removeFirstOccurrence(this);
                    }
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            if (this.requestContext != null) {
                RequestContext<?> requestContext = this.requestContext;
                this.requestContext = null;
                if (isOpen() && HttpUtil.isKeepAlive(msg)) {
                    cachedPool.offerLast(this);
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
                // Just to be on the safe side, always close channel and remove it from cached
                // pool.
                ctx.close();
                cachedPool.removeFirstOccurrence(this);
            }
        }

        @Override
        public InetSocketAddress address() {
            return address;
        }

        @Override
        public Channel channel() {
            return channel;
        }

        @Override
        public void sendAsnyc(RequestContext<?> requestContext) {
            final Channel channel = channel();
            if (channel.isActive()) {
                channel.eventLoop().execute(() -> {
                    Request request = requestContext.request;
                    if (channel.isActive()) {
                        this.requestContext = requestContext;
                        HttpMethod method = request.method();
                        HttpHeaders headers = request.headers();
                        URI uri = request.uri();
                        String path = uri.getRawPath();
                        String query = uri.getRawQuery();
                        String requestUri = query == null ? path : path + "?" + query;
                        ByteBuf content = request.contentHolder().content(channel.alloc());
                        DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method,
                                requestUri, content, headers, request.trailingHeaders());
                        headers.set(HOST, headerHost);
                        if (method == POST || method == PUT || method == PATCH || method == DELETE) {
                            int contentLength = content.readableBytes();
                            headers.setInt(CONTENT_LENGTH, contentLength);
                            if (!headers.contains(CONTENT_TYPE)) {
                                headers.set(CONTENT_TYPE, contentType(APPLICATION_X_WWW_FORM_URLENCODED));
                            }
                        }
                        if (compressionEnabled) {
                            headers.set(ACCEPT_ENCODING, GZIP_DEFLATE);
                        } else {
                            headers.remove(ACCEPT_ENCODING);
                        }
                        HttpUtil.setKeepAlive(req, true);
                        channel.writeAndFlush(req);
                    } else {
                        requestContext.future.completeExceptionally(new ClosedChannelException());
                    }
                });
            } else {
                requestContext.future.completeExceptionally(new ClosedChannelException());
            }
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
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder extends AbstractBuilder<DefaultHttpClient, Builder> {

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
            return new DefaultHttpClient(transportLibrary.createGroup(0, threadFactory),
                    transportLibrary.channelClass(), sslContextProvider, compressionEnabled, true, timeoutSeconds(),
                    maxContentLength);
        }

        /**
         * Returns a new {@link DefaultHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
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
         * 
         * @param group        the {@link EventLoopGroup}
         * @param channelClass the {@link Class} of {@link Channel}
         * @return a new {@code ConnectionCachedHttpClient}
         */
        public DefaultHttpClient build(EventLoopGroup group, Class<? extends Channel> channelClass) {
            ensureSslContext();
            return new DefaultHttpClient(group, channelClass, sslContextProvider, compressionEnabled, false, timeoutSeconds(),
                    maxContentLength);
        }

    }

}