package com.github.fmjsjx.libnetty.handler.timeout;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Raise an {@link AllTimeoutException} when no data was either read or
 * written within a certain period of time.
 *
 * @author MJ Fang
 * @since 4.1
 */
public class AllTimeoutHandler extends IdleStateHandler {

    private boolean closed;

    /**
     * Creates a new {@link AllTimeoutHandler} instance.
     *
     * @param timeoutSeconds all timeout in seconds
     */
    public AllTimeoutHandler(long timeoutSeconds) {
        super(0, 0, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Creates a new {@link AllTimeoutHandler} instance.
     *
     * @param timeout all timeout
     * @param unit    the {@link TimeUnit} of {@code timeout}
     */
    public AllTimeoutHandler(long timeout, TimeUnit unit) {
        super(0, 0, timeout, unit);
    }

    /**
     * Called when an {@link IdleStateEvent} with state
     * {@link IdleState#ALL_IDLE} should be fired.
     *
     * @param ctx the current {@link ChannelHandlerContext}
     * @param evt the {@link IdleStateEvent} that was fired
     */
    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        allTimedOut(ctx);
    }

    /**
     * Called when an all timeout was detected.
     *
     * @param ctx the current {@link ChannelHandlerContext}
     */
    protected void allTimedOut(ChannelHandlerContext ctx) {
        if (!closed) {
            ctx.fireExceptionCaught(AllTimeoutException.getInstance());
            ctx.close();
            closed = true;
        }
    }

}
