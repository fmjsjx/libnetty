package com.github.fmjsjx.libnetty.http.server;

import java.util.List;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;

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
        DecoderResult decoderResult = msg.decoderResult();
        if (decoderResult.isFailure()) {
            // Just respond 400 Bad Request
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            ChannelFuture cf = ctx.writeAndFlush(HttpResponseUtil.badRequest(msg.protocolVersion(), keepAlive));
            if (!keepAlive) {
                cf.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            out.add(new DefaultHttpRequestContext(ctx.channel(), msg.retain()));
        }
    }

}
