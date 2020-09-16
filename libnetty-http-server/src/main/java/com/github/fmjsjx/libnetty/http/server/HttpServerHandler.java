package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandler.Sharable;

/**
 * An interface handles HTTP requests.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface HttpServerHandler extends ChannelInboundHandler {

    /**
     * A {@link ChannelFutureListener} that calls {@link Channel#read()} of the
     * {@link Channel} which is associated with the specified {@link ChannelFuture}.
     */
    ChannelFutureListener READ_NEXT = f -> f.channel().read();

    /**
     * Return {@code true} if the implementation is {@link Sharable} and so can be
     * added to different {@link ChannelPipeline}s.
     * 
     * @return {@code true} if the implementation is {@link Sharable}
     */
    boolean isSharable();

}
