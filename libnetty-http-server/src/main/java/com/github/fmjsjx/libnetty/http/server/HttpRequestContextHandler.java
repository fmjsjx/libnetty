package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * An abstract handler to handle {@link HttpRequestContext} messages.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public abstract class HttpRequestContextHandler extends SimpleChannelInboundHandler<HttpRequestContext>
        implements HttpServerHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequestContext msg) throws Exception {
        messageReceived(ctx, msg);
    }

    protected abstract void messageReceived(ChannelHandlerContext ctx, HttpRequestContext msg) throws Exception;

}
