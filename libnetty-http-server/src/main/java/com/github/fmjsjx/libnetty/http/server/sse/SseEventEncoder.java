package com.github.fmjsjx.libnetty.http.server.sse;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Encodes SSE events to {@link io.netty.buffer.ByteBuf}s.
 * <p>
 * This encoder is {@code sharable}.
 *
 * @author MJ Fang
 * @since 3.9
 */
@Sharable
public class SseEventEncoder extends MessageToMessageEncoder<SseEventSerializable> {


    private static final class InstanceHolder {
        private static final SseEventEncoder INSTANCE = new SseEventEncoder();
    }

    /**
     * Returns the singleton instance of {@link SseEventEncoder}.
     *
     * @return the singleton instance of {@link SseEventEncoder}
     */
    public static SseEventEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Constructs a new {@link SseEventEncoder} instance.
     */
    public SseEventEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SseEventSerializable msg, List<Object> out) {
        var buf = msg.serialize(ctx.alloc());
        out.add(buf);
    }

}
