package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

/**
 * The abstract implementation of {@link FcgiContent}.
 *
 * @param <Self> the type of the super class
 * @author MJ Fang
 * @since 1.0
 */
public abstract class AbstractFcgiContent<Self extends AbstractFcgiContent<?>> extends AbstractFcgiRecord
        implements FcgiContent {

    protected final ByteBuf content;

    protected AbstractFcgiContent(FcgiVersion protocolVersion, int requestId, ByteBuf content) {
        super(protocolVersion, requestId);
        this.content = Objects.requireNonNull(content, "content must not be null");
    }

    @Override
    public ByteBuf content() {
        return content;
    }

    @Override
    protected String bodyToString() {
        return content.toString(CharsetUtil.UTF_8);
    }

    @Override
    public abstract Self replace(ByteBuf content);

    @Override
    public Self copy() {
        return replace(content.copy());
    }

    @Override
    public Self duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public Self retainedDuplicate() {
        return replace(content.retainedDuplicate());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Self retain() {
        content.retain();
        return (Self) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Self retain(int increment) {
        content.retain(increment);
        return (Self) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Self touch() {
        content.touch();
        return (Self) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Self touch(Object hint) {
        content.touch(hint);
        return (Self) this;
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

}
