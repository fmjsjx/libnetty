package com.github.fmjsjx.libnetty.http.client;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.proxy.ProxyConnectionEvent;

class ProxyEventHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyEventHandler.class);

    private final BiConsumer<ChannelHandlerContext, Object> nextAction;

    ProxyEventHandler(BiConsumer<ChannelHandlerContext, Object> nextAction) {
        this.nextAction = nextAction;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        nextAction.accept(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ProxyConnectionEvent) {
            ctx.pipeline().remove(this);
            logger.debug("Remove this handler and accpet the next action with event: {}", evt);
            nextAction.accept(ctx, evt);
        }
    }

//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
//        if (HttpCommonUtil.isSuccess(msg)) {
//            ctx.pipeline().remove(this);
//            logger.debug("Remove this handler and accpet the next action");
//            nextAction.accept(ctx, null);
//        } else {
//            ctx.close();
//            nextAction.accept(ctx, new HttpRuntimeException("Connect response not OK: " + msg.status()));
//        }
//    }

}
