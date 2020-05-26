package com.github.fmjsjx.libnetty.http.client;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.util.ReferenceCountUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A common implementation of {@link HttpClient} which will cache {@code TCP}
 * connections.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
@Slf4j
public class ConnectionCachedHttpClient extends AbstractHttpClient {

    private final boolean shutdownGroupOnClose;
    private final int timeoutSeconds;
    private final int maxContentLength;

    ConnectionCachedHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContext sslContext,
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
        return null;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class RequestContext {

        private final CompletableFuture<Object> future;
        private final HttpContentHandler<?> contentHandler;
        private final Optional<Executor> executor;

    }

    private interface HttpConnection {

        InetSocketAddress address();

        Channel channel();

        HttpClientHandler handler();

        default boolean isActive() {
            Channel channel = channel();
            return channel != null && channel.isActive();
        }

        void send(Request request, RequestContext requestContext);

    }

    @RequiredArgsConstructor
    private final class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse>
            implements HttpConnection {

        private final InetSocketAddress address;
        private final CharSequence headerHost;
        private final ConcurrentLinkedDeque<HttpConnection> cachePool;
        private volatile Channel channel;

        private RequestContext requestContext;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            this.channel = ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
            if (requestContext != null) {
                if (!requestContext.future.isDone()) {
                    requestContext.future.completeExceptionally(cause);
                }
            } else {
                // remove HttpConnection from cache pool
                cachePool.removeFirstOccurrence(this);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            RequestContext requestContext = this.requestContext;
            this.requestContext = null;
            if (requestContext.executor.isPresent()) {
                msg.retain();
                requestContext.executor.get().execute(() -> {
                    try {
                        DefaultResponse<?> response = new DefaultResponse<>(msg.protocolVersion(), msg.status(),
                                msg.headers(), requestContext.contentHandler.apply(msg.content()));
                        requestContext.future.complete(response);
                    } finally {
                        msg.release();
                    }
                });
            } else {
                DefaultResponse<?> response = new DefaultResponse<>(msg.protocolVersion(), msg.status(), msg.headers(),
                        requestContext.contentHandler.apply(msg.content()));
                requestContext.future.complete(response);
            }
            if (HttpUtil.isKeepAlive(msg)) {
                cachePool.offerLast(this);
            } else {
                ctx.close();
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
        public HttpClientHandler handler() {
            return this;
        }

        @Override
        public void send(Request request, RequestContext requestContext) {
            final Channel channel = this.channel;
            channel.eventLoop().execute(() -> {
                ByteBuf content = request.content();
                if (channel.isActive()) {
                    this.requestContext = requestContext;
                    HttpHeaders headers = request.headers();
                    URI uri = request.uri();
                    String path = uri.getRawPath();
                    String query = uri.getRawQuery();
                    String requestUri = query == null ? path : path + "?" + query;
                    DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, request.method(),
                            requestUri, content, headers, request.trailingHeaders());
                    headers.set(HOST, headerHost);
                    int contentLength = content.readableBytes();
                    if (contentLength > 0) {
                        headers.setInt(CONTENT_LENGTH, contentLength);
                        if (!headers.contains(CONTENT_TYPE)) {
                            headers.set(CONTENT_TYPE, contentType(APPLICATION_X_WWW_FORM_URLENCODED));
                        }
                    }
                    HttpUtil.setKeepAlive(req, false);
                    channel.writeAndFlush(req);
                } else {
                    ReferenceCountUtil.safeRelease(content);
                    requestContext.future.completeExceptionally(new IOException("socket channel closed"));
                }
            });
        }

    }

}
