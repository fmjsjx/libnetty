package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
class DefaultHttpServerHandler extends HttpRequestContextHandler {

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpRequestContext msg) throws Exception {
        // TODO Auto-generated method stub

    }
    
    void onServerClosed() {
        // TODO
    }

}
