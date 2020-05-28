package com.github.fmjsjx.libnetty.http.client;

import static com.github.fmjsjx.libnetty.http.HttpUtil.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple implementation of {@link HttpClient} uses short connections (create
 * and close channel for each request).
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 * 
 * @see AbstractHttpClient
 * @see ConnectionCachedHttpClient
 */
@Slf4j
public class SimpleHttpClient extends AbstractHttpClient {

    /**
     * Builder of {@link SimpleHttpClient}.
     * 
     * @since 1.0
     * 
     * @author MJ Fang
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
            ThreadFactory threadFactory = new DefaultThreadFactory(SimpleHttpClient.class);
            return new SimpleHttpClient(transportLibrary.createGroup(0, threadFactory), transportLibrary.channelClass(),
                    sslContext, true, timeoutSeconds(), maxContentLength);
        }

        /**
         * Returns a new {@link SimpleHttpClient} built from the current state of this
         * builder with given {@link EventLoopGroup}.
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
         * 
         * @param group        the {@link EventLoopGroup}
         * @param channelClass the {@link Class} of {@link Channel}
         * @return a new {@code SimpleHttpClient}
         */
        public SimpleHttpClient build(EventLoopGroup group, Class<? extends Channel> channelClass) {
            ensureSslContext();
            return new SimpleHttpClient(group, channelClass, sslContext, false, timeoutSeconds(), maxContentLength);
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
    private final int timeoutSeconds;
    private final int maxContentLength;

    SimpleHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContext sslContext,
            boolean shutdownGroupOnClose, int timeoutSeconds, int maxContentLength) {
        super(group, channelClass, sslContext);
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
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        String requestUri = query == null ? path : path + "?" + query;
        b.connect(address).addListener((ChannelFuture cf) -> {
            ByteBuf content = request.content();
            if (cf.isSuccess()) {
                HttpHeaders headers = request.headers();
                DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, request.method(),
                        requestUri, content, headers, request.trailingHeaders());
                headers.set(HOST, defaultPort ? host : host + ":" + port);
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
                ReferenceCountUtil.safeRelease(content);
                future.completeExceptionally(cf.cause());
            }
        });
        return future;
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
