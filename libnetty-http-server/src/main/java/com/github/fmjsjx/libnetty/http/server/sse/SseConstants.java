package com.github.fmjsjx.libnetty.http.server.sse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * Defines static constants.
 *
 * @author MJ Fang
 * @since 3.9
 */
class SseConstants {

    static final ByteBuf LABEL_EVENT = Unpooled.unreleasableBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(7, 7).writeBytes("event: ".getBytes()).asReadOnly());
    static final ByteBuf LABEL_ID = Unpooled.unreleasableBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(4, 4).writeBytes("id: ".getBytes()).asReadOnly());
    static final ByteBuf LABEL_DATA = Unpooled.unreleasableBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(6, 6).writeBytes("data: ".getBytes()).asReadOnly());

    static final ByteBuf EVENT_PING = Unpooled.unreleasableBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(13, 13).writeBytes("event: ping\n\n".getBytes()).asReadOnly());

    private SseConstants() {
    }

}
