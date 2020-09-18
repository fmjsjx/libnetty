package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AsciiString;

@Sharable
class HstsHandler extends ChannelOutboundHandlerAdapter {

    private static final int DEFAULT_MAX_AGE = 86400;

    private static final AsciiString STRICT_TRANSPORT_SECURITY = AsciiString.cached("strict-transport-security");

    private static final class InstanceHolder {
        private static final HstsHandler instance = new HstsHandler();
    }

    public static final HstsHandler getInstance() {
        return InstanceHolder.instance;
    }

    private final AsciiString value;

    HstsHandler() {
        this(DEFAULT_MAX_AGE);
    }

    HstsHandler(int maxAge) {
        value = AsciiString.cached("max-age=" + maxAge);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            res.headers().set(STRICT_TRANSPORT_SECURITY, value);
        }
        super.write(ctx, msg, promise);
    }

}
