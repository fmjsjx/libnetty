package com.github.fmjsjx.libnetty.http.server;

import java.util.List;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.FullHttpRequest;

@Sharable
class HttpRequestContextEncoder extends MessageToMessageEncoder<FullHttpRequest> {

    @Override
    protected void encode(ChannelHandlerContext ctx, FullHttpRequest msg, List<Object> out) throws Exception {
        out.add(new DefaultHttpRequestContext(ctx.channel(), msg.retain()));
    }

}
