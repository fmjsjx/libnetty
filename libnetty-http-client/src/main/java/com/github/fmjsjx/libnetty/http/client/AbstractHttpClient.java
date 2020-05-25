package com.github.fmjsjx.libnetty.http.client;

import java.util.Objects;

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
    protected final SslContext sslContext;

    protected AbstractHttpClient(EventLoopGroup group, SslContext sslContext) {
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.sslContext = Objects.requireNonNull(sslContext, "sslContext must not be null");
    }
    
    protected EventLoopGroup group() {
        return this.group;
    }

    @Override
    public SslContext sslContext() {
        return sslContext;
    }

}
