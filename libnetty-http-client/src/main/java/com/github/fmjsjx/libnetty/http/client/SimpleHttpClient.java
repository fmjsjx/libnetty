package com.github.fmjsjx.libnetty.http.client;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.github.fmjsjx.libnetty.http.client.exception.ClientClosedException;
import com.github.fmjsjx.libnetty.transport.TransportLibrary;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple implementation of {@link HttpClient} uses short connections (create
 * and close channel for each request).
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
@Slf4j
public class SimpleHttpClient implements HttpClient {

    /**
     * Builder of {@link SimpleHttpClient}.
     * 
     * @since 1.0
     * 
     * @author fmjsjx
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {

        /**
         * Default is 60 seconds.
         */
        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

        /**
         * Default is 1 MB.
         */
        private static final int DEFAULT_MAX_CONTENT_LENGTH = 1 * 1024 * 1024;

        private Duration timeout = DEFAULT_TIMEOUT;

        private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;

        private SslContext sslContext;

        /**
         * Returns the timeout duration for this client.
         * 
         * @return the timeout {@link Duration}
         */
        public Duration timeout() {
            return timeout;
        }

        /**
         * Sets the timeout duration for this client.
         * 
         * @param duration the timeout {@link Duration}
         * @return this {@link Builder}
         */
        public Builder timeout(Duration duration) {
            timeout = duration == null ? DEFAULT_TIMEOUT : duration;
            return this;
        }

        /**
         * Returns the SSL context for this client.
         * 
         * @return the {@link SslContext}
         */
        public SslContext sslContext() {
            return sslContext;
        }

        /**
         * Sets the SSL context for this client.
         * 
         * @param sslContext the {@link SslContext}
         * @return this {@link Builder}
         */
        public Builder sslContext(SslContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Returns the max content length for this client.
         * 
         * @return the max content length
         */
        public int maxContentLength() {
            return maxContentLength;
        }

        /**
         * Sets the max content length for this client.
         * 
         * @param maxContentLength the max content length
         * @return this {@link Builder}
         */
        public Builder maxContentLength(int maxContentLength) {
            if (maxContentLength > 0) {
                this.maxContentLength = maxContentLength;
            }
            return this;
        }

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with internal {@link EventLoopGroup}.
         * 
         * @return a new {@link SimpleHttpClient}
         */
        public SimpleHttpClient build() {
            ensureSslContext();
            return new SimpleHttpClient(TransportLibrary.getDefault().createGroup(), sslContext, true, timeoutSeconds(),
                    maxContentLength);
        }

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
         * 
         * @param group the {@link EventLoopGroup}
         * @return a new {@link SimpleHttpClient}
         */
        public SimpleHttpClient build(EventLoopGroup group) {
            ensureSslContext();
            return new SimpleHttpClient(group, sslContext, false, timeoutSeconds(), maxContentLength);
        }

        private void ensureSslContext() {
            if (sslContext == null) {
                sslContext = SslContextUtil.createForClient();
            }
        }

        private int timeoutSeconds() {
            return (int) timeout.getSeconds();
        }

    }

    /**
     * Returns a new {@link Builder} with default settings.
     * 
     * @return a {@link Builder}.
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link SimpleHttpClient} with default settings.
     * 
     * @return a {@link SimpleHttpClient}
     */
    public static final SimpleHttpClient build() {
        return builder().build();
    }

    private final EventLoopGroup group;
    private final SslContext sslContext;
    private final boolean shutdownGroupOnClose;
    private final int timeoutSeconds;
    private final int maxContentLength;

    private final Class<? extends SocketChannel> channelClass;

    private volatile boolean closed;

    SimpleHttpClient(EventLoopGroup group, SslContext sslContext, boolean shutdownGroupOnClose, int timeoutSeconds,
            int maxContentLength) {
        this.group = group;
        this.sslContext = sslContext;
        this.shutdownGroupOnClose = shutdownGroupOnClose;
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.channelClass = SocketChannelUtil.fromEventLoopGroup(group);
    }

    @Override
    public SslContext sslContext() {
        return sslContext;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            try {
                close0();
            } finally {
                closed = true;
            }
        }
    }

    private void close0() {
        if (shutdownGroupOnClose) {
            log.debug("Shutdown {}", group);
            group.shutdownGracefully();
        }
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler) {
        ensureOpen();
        return sendAsync0(request, contentHandler, null);
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler,
            Executor executor) {
        ensureOpen();
        return sendAsync0(request, contentHandler, executor);
    }

    private <T> CompletableFuture<Response<T>> sendAsync0(Request request, HttpContentHandler<T> contentHandler,
            Executor executor) {
        URI uri = request.uri();
        boolean ssl = "https".equals(uri.getScheme());
        int port = uri.getPort() == -1 ? (ssl ? 443 : 80) : uri.getPort();
        String host = uri.getHost();
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
        CompletableFuture<Response<T>> future = new CompletableFuture<>();
        Bootstrap b = new Bootstrap().group(group).channel(channelClass).option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline cp = ch.pipeline();
                        cp.addLast(new ReadTimeoutHandler(timeoutSeconds));
                        if (ssl) {
                            cp.addLast(sslContext.newHandler(ch.alloc(), host, port));
                        }
                        cp.addLast(new HttpClientCodec());
                        cp.addLast(new HttpObjectAggregator(maxContentLength));
                        cp.addLast(new SimpleHttpClientHandler<>(future, contentHandler, executor));
                    }
                });
        String path = uri.getPath();
        b.connect(address).addListener((ChannelFuture cf) -> {
            if (cf.isDone()) {
                if (cf.isSuccess()) {
                    HttpHeaders headers = request.headers();
                    ByteBuf content = request.content();
                    DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, request.method(),
                            path, content, headers, request.trailingHeaders());
                    headers.set(HOST, host);
                    int contentLength = content.readableBytes();
                    if (contentLength > 0) {
                        headers.setInt(CONTENT_LENGTH, contentLength);
                        if (!headers.contains(CONTENT_TYPE)) {
                            headers.set(CONTENT_TYPE, contentType(APPLICATION_X_WWW_FORM_URLENCODED));
                        }
                    }
                    HttpUtil.setKeepAlive(req, false);
                    cf.channel().writeAndFlush(req);
                } else {
                    future.completeExceptionally(cf.cause());
                }
            }
        });
        return future;
    }

    private void ensureOpen() {
        if (closed) {
            throw new ClientClosedException(toString() + " already closed");
        }
    }

    private static final class SimpleHttpClientHandler<T> extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CompletableFuture<Response<T>> future;
        private final HttpContentHandler<T> contentHandler;
        private final Executor executor;

        private SimpleHttpClientHandler(CompletableFuture<Response<T>> future, HttpContentHandler<T> contentHandler,
                Executor executor) {
            this.future = future;
            this.contentHandler = contentHandler;
            this.executor = executor;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (!future.isDone()) {
                future.completeExceptionally(cause);
            }
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (!future.isDone()) {
                future.completeExceptionally(new IllegalStateException("No Response Content"));
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
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
            ctx.close();
        }

        private Response<T> buildResponse(FullHttpResponse msg) {
            return new DefaultResponse<>(msg.protocolVersion(), msg.status(), msg.headers(),
                    contentHandler.apply(msg.content()));
        }

    }

}