package com.github.fmjsjx.libnetty.http.client;

import java.util.Objects;

import com.github.fmjsjx.libnetty.http.client.exception.ClientClosedException;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;

/**
 * Abstract implementation of {@link HttpClient}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class AbstractHttpClient implements HttpClient {

    protected final EventLoopGroup group;
    protected final Class<? extends Channel> channelClass;
    protected final SslContext sslContext;

    protected volatile boolean closed;

    protected AbstractHttpClient(EventLoopGroup group, Class<? extends Channel> channelClass, SslContext sslContext) {
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.channelClass = Objects.requireNonNull(channelClass, "channelClass must not be null");
        this.sslContext = Objects.requireNonNull(sslContext, "sslContext must not be null");
    }

    protected EventLoopGroup group() {
        return this.group;
    }

    protected Class<? extends Channel> channelClass() {
        return channelClass;
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

    protected abstract void close0();

    protected void ensureOpen() {
        if (closed) {
            throw new ClientClosedException(toString() + " already closed");
        }
    }

}
