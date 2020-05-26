package com.github.fmjsjx.libnetty.http.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.ssl.SslContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
    private static final class RequestContext<T> {

        private final CompletableFuture<Response<T>> future;
        private final HttpContentHandler<T> contentHandler;
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

    }

    private final class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse>
            implements HttpConnection {

        private volatile RequestContext<?> requestContext;
        private volatile Channel channel;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public InetSocketAddress address() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Channel channel() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HttpClientHandler handler() {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
