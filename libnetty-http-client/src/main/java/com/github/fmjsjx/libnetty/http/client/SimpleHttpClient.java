package com.github.fmjsjx.libnetty.http.client;

import static com.github.fmjsjx.libnetty.http.HttpCommonUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.netty.handler.codec.http.HttpHeaderValues.GZIP_DEFLATE;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

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
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
public class SimpleHttpClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpClient.class);

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
            ThreadFactory threadFactory = new DefaultThreadFactory(SimpleHttpClient.class, true);
            return new SimpleHttpClient(transportLibrary.createGroup(ioThreads, threadFactory),
                    transportLibrary.channelClass(), sslContextProvider, compressionEnabled, brotliEnabled, true,
                    timeoutSeconds(), maxContentLength);
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
            return new SimpleHttpClient(group, channelClass, sslContextProvider, compressionEnabled, brotliEnabled,
                    false, timeoutSeconds(), maxContentLength);
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

    SimpleHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContextProvider sslContextProvider,
            boolean compressionEnabled, boolean brotliEnabled, boolean shutdownGroupOnClose, int timeoutSeconds,
            int maxContentLength) {
        super(group, channelClass, sslContextProvider, compressionEnabled, brotliEnabled);
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
                            cp.addLast(sslContextProvider.get().newHandler(ch.alloc(), host, port));
                        }
                        cp.addLast(new HttpClientCodec());
                        cp.addLast(new HttpContentDecompressor());
                        cp.addLast(new HttpObjectAggregator(maxContentLength));
                        if (brotliEnabled) {
                            cp.addLast(BrotliDecompressor.INSTANCE);
                        }
                        cp.addLast(new SimpleHttpClientHandler<>(future, contentHandler, executor));
                    }
                });
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        String requestUri = query == null ? path : path + "?" + query;
        b.connect(address).addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                HttpMethod method = request.method();
                HttpHeaders headers = request.headers();
                ByteBuf content = request.contentHolder().content(cf.channel().alloc());
                DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, requestUri,
                        content, headers, request.trailingHeaders());
                headers.set(HOST, defaultPort ? host : host + ":" + port);
                if (method == POST || method == PUT || method == PATCH || method == DELETE) {
                    int contentLength = content.readableBytes();
                    headers.setInt(CONTENT_LENGTH, contentLength);
                    if (!headers.contains(CONTENT_TYPE)) {
                        headers.set(CONTENT_TYPE, contentType(APPLICATION_X_WWW_FORM_URLENCODED));
                    }
                }
                if (compressionEnabled) {
                    headers.set(ACCEPT_ENCODING, brotliEnabled ? GZIP_DEFLATE_BR : GZIP_DEFLATE);
                } else {
                    headers.remove(ACCEPT_ENCODING);
                }
                HttpUtil.setKeepAlive(req, false);
                cf.channel().writeAndFlush(req);
            } else {
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
