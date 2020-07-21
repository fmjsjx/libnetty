package com.github.fmjsjx.libnetty.http.server;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;

class AutoReadNextHandler extends ChannelOutboundHandlerAdapter {

    static ChannelFutureListener READ_NEXT = f -> f.channel().read();

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
