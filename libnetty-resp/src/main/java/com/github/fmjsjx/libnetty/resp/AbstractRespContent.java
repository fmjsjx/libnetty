package com.github.fmjsjx.libnetty.resp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * The abstract implementation of the {@link RespContent}.
 * 
 * @param <Self> the type of the Super Class
 * 
 * @since 1.0
 *
 * @author fmjsjx
 */
public abstract class AbstractRespContent<Self extends RespContent> implements RespContent {

    protected final ByteBuf content;

    protected AbstractRespContent(ByteBuf content) {
        this.content = Objects.requireNonNull(content, "content must not be null");
    }

    protected AbstractRespContent() {
        this.content = Unpooled.EMPTY_BUFFER;
    }

    @Override
    public Integer toInteger() {
        return RespCodecUtil.decodeInt(content);
    }

    @Override
    public Long toLong() {
        return RespCodecUtil.decodeLong(content);
    }

    @Override
    public Double toDouble() {
        return Double.valueOf(toText(CharsetUtil.US_ASCII));
    }

    @Override
    public BigInteger toBigInteger() {
        return new BigInteger(toText(CharsetUtil.US_ASCII));
    }

    @Override
    public BigDecimal toBigDecimal() {
        return new BigDecimal(toText(CharsetUtil.US_ASCII));
    }

    @Override
    public String toText(Charset charset) {
        return content.toString(charset);
    }

    @Override
    public AsciiString toAscii() {
        return new AsciiString(content.nioBuffer());
    }

    @Override
    public ByteBuf content() {
        return content;
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

    @Override
    public abstract Self replace(ByteBuf content);

    @Override
    @SuppressWarnings("unchecked")
    public Self retain() {
        content.retain();
        return (Self) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Self retain(int increment) {
        content.retain(increment);
        return (Self) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Self touch() {
        content.touch();
        return (Self) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Self touch(Object hint) {
        content.touch(hint);
        return (Self) this;
    }

}
