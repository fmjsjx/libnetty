package com.github.fmjsjx.libnetty.http.server;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;

@Sharable
class AutoReadNextHandler extends ChannelDuplexHandler {

    private static final class InstanceHolder {
        private static final AutoReadNextHandler instance = new AutoReadNextHandler();
    }

    static final AutoReadNextHandler getInstance() {
        return InstanceHolder.instance;
    }

    static ChannelFutureListener READ_NEXT = f -> f.channel().read();

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse resp = (HttpResponse) msg;
            if (isKeepAlive(resp)) {
                promise.addListener(READ_NEXT);
            }
        }
        super.write(ctx, msg, promise);
    }

}
