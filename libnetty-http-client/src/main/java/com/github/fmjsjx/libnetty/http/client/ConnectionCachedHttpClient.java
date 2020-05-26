package com.github.fmjsjx.libnetty.http.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;

/**
 * A common implementation of {@link HttpClient} which will cache {@code TCP}
 * connections.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class ConnectionCachedHttpClient extends AbstractHttpClient {

    ConnectionCachedHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContext sslContext) {
        super(group, channelClass, sslContext);
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler,
            Executor executor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void close0() {
        // TODO Auto-generated method stub

    }

}
