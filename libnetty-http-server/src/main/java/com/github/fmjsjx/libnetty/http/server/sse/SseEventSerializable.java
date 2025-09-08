package com.github.fmjsjx.libnetty.http.server.sse;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Interface defines method for serialization of SSE events.
 *
 * <p>Classes implementing this interface know how to serialize itself.
 *
 * @author MJ Fang
 * @since 3.9
 */
public interface SseEventSerializable {

    /**
     * Serialize this event into the specified {@link ByteBuf} given.
     *
     * @param out a {@link ByteBuf}
     */
    void serialize(ByteBuf out);

    /**
     * Serialize this event into a {@link ByteBuf} which allocated by
     * the specified {@link ByteBufAllocator} given.
     *
     * @param allocator the {@link ByteBufAllocator}
     * @return the {@link ByteBuf} allocated by the {@code allocator}
     */
    default ByteBuf serialize(ByteBufAllocator allocator) {
        var byteBuf = allocator.buffer();
        serialize(byteBuf);
        return byteBuf;
    }

}
