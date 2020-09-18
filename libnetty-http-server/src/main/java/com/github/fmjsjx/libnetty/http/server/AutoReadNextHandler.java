package com.github.fmjsjx.libnetty.http.server;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;

@Sharable
class AutoReadNextHandler extends ChannelOutboundHandlerAdapter {

    private static final class InstanceHolder {
        private static final AutoReadNextHandler instance = new AutoReadNextHandler();
    }

    static final AutoReadNextHandler getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse resp = (FullHttpResponse) msg;
            if (isKeepAlive(resp)) {
                promise.addListener(HttpServerHandler.READ_NEXT);
            }
        }
        super.write(ctx, msg, promise);
    }

}
