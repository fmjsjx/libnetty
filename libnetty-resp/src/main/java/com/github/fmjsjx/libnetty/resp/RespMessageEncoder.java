package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

@Sharable
public class RespMessageEncoder extends MessageToMessageEncoder<RespMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RespMessage msg, List<Object> out) throws Exception {
        msg.encode(ctx.alloc(), out);
    }

}
