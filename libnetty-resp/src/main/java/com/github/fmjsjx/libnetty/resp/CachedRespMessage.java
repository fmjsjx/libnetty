package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * The abstract implementation of the cached {@link RespMessage}.
 *
 * @since 1.1
 * 
 * @author MJ Fang
 */
public abstract class CachedRespMessage implements RespMessage {

    protected final ByteBuf fullContent;

    protected CachedRespMessage(ByteBuf fullContent) {
        this.fullContent = Unpooled.unreleasableBuffer(fullContent.asReadOnly());
    }

    protected ByteBuf fullContent() {
        return fullContent.duplicate();
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        out.add(fullContent());
    }

}
