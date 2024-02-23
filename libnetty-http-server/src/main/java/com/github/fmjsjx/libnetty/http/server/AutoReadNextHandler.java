package com.github.fmjsjx.libnetty.http.server;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * A special {@link ChannelOutboundHandler} which support the {@code AUTO_READ}
 * feature for kept alive {@link Channel}s which just send a
 * {@link FullHttpResponse} to client.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Sharable
public class AutoReadNextHandler extends ChannelOutboundHandlerAdapter {

    private static final class InstanceHolder {
        private static final AutoReadNextHandler instance = new AutoReadNextHandler();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton instance
     */
    public static final AutoReadNextHandler getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FullHttpResponse resp) {
            if (isKeepAlive(resp)) {
                promise.addListener(HttpServerHandler.READ_NEXT);
            }
        }
        super.write(ctx, msg, promise);
    }

}
