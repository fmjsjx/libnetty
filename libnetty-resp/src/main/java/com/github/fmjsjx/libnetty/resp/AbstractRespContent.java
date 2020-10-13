package com.github.fmjsjx.libnetty.resp;

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * The abstract implementation of the {@link RespContent}.
 * 
 * @param <Self> the type of the Super Class
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class AbstractRespContent<Self extends RespContent> implements RespContent {

    protected final ByteBuf data;

    protected AbstractRespContent(ByteBuf content) {
        this.data = Objects.requireNonNull(content, "content must not be null");
    }

    protected AbstractRespContent() {
        this.data = Unpooled.EMPTY_BUFFER;
    }

    @Override
    public ByteBuf content() {
        return ByteBufUtil.ensureAccessible(data);
    }

    @Override
    public int refCnt() {
        return data.refCnt();
    }

    @Override
    public boolean release() {
        return data.release();
    }

    @Override
    public boolean release(int decrement) {
        return data.release(decrement);
    }

    @Override
    public Self copy() {
        return replace(data.copy());
    }

    @Override
    public Self duplicate() {
        return replace(data.duplicate());
    }

    @Override
    public Self retainedDuplicate() {
        return replace(data.retainedDuplicate());
    }

    @Override
    public abstract Self replace(ByteBuf content);

    @Override
    @SuppressWarnings("unchecked")
    public Self retain() {
        data.retain();
        return (Self) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Self retain(int increment) {
        data.retain(increment);
        return (Self) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Self touch() {
        data.touch();
        return (Self) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Self touch(Object hint) {
        data.touch(hint);
        return (Self) this;
    }

}
