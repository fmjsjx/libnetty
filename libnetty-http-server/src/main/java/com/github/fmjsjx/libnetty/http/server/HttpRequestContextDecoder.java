package com.github.fmjsjx.libnetty.http.server;

import java.util.List;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;

@Sharable
class HttpRequestContextDecoder extends MessageToMessageDecoder<FullHttpRequest> {

    private static final class InstanceHolder {
        private static final HttpRequestContextDecoder instance = new HttpRequestContextDecoder();
    }

    static final HttpRequestContextDecoder getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest msg, List<Object> out) throws Exception {
        out.add(new DefaultHttpRequestContext(ctx.channel(), msg.retain()));
    }

}
