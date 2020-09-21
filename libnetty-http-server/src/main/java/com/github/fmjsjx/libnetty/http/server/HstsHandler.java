package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.ChannelHandler.Sharable;

import java.time.Duration;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AsciiString;

/**
 * A special {@link ChannelOutboundHandler} which sets the HTTP
 * {@code Strict-Transport-Security} header automatically.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Sharable
public class HstsHandler extends ChannelOutboundHandlerAdapter {

    private static final int DEFAULT_MAX_AGE = 86400;

    private static final AsciiString STRICT_TRANSPORT_SECURITY = AsciiString.cached("strict-transport-security");

    private static final class InstanceHolder {
        private static final HstsHandler instance = new HstsHandler();
    }

    /**
     * Returns the singleton instance with default {@code max-age}.
     * 
     * @return the singleton instance with default {@code max-age}
     */
    public static final HstsHandler getInstance() {
        return InstanceHolder.instance;
    }

    private final AsciiString value;

    /**
     * Constructs a new {@link HstsHandler} instance with default {@code max-age}
     * ({@code 86400}).
     */
    public HstsHandler() {
        this(DEFAULT_MAX_AGE);
    }

    /**
     * Constructs a new {@link HstsHandler} instance with the specified
     * {@code maxAge}.
     * 
     * @param maxAge the {@code max-age} value in seconds
     */
    public HstsHandler(long maxAge) {
        value = AsciiString.cached("max-age=" + maxAge);
    }

    /**
     * Constructs a new {@link HstsHandler} instance with the specified
     * {@code maxAge}.
     * 
     * @param maxAge the {@code max-age} value
     */
    public HstsHandler(Duration maxAge) {
        this(maxAge.getSeconds());
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
