package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * Encodes {@link RespMessage}s to {@link ByteBuf}s.
 * 
 * <p>
 * This encoder is {@code sharable}.
 *
 * @since 1.0
 *
 * @author MJ Fang
 */
@Sharable
public class RespMessageEncoder extends MessageToMessageEncoder<RespMessage> {

    private static final class InstanceHolder {
        private static final RespMessageEncoder INSTANCE = new RespMessageEncoder();
    }

    /**
     * Returns the singleton {@link RespMessageEncoder} instance.
     *
     * @return the singleton {@link RespMessageEncoder} instance
     * @since 4.0
     */
    public static RespMessageEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Constructs a new {@link RespMessageEncoder} instance.
     */
    public RespMessageEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RespMessage msg, List<Object> out) throws Exception {
        msg.encode(ctx.alloc(), out);
    }

}
