package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;

/**
 * The cached implementation of {@link RespBulkStringMessage} with {@code nil}
 * value.
 * 
 * <p>
 * This class is implemented in singleton pattern.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class CachedNullMessage extends CachedRespMessage implements RespBulkStringMessage {

    private static final CachedNullMessage instance = new CachedNullMessage();

    /**
     * Returns the <b>SINGLETON</b> {@link CachedNullMessage} instance.
     * 
     * @return a {@code CachedNullMessage}
     */
    public static final CachedNullMessage instance() {
        return instance;
    }

    private CachedNullMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + NULL_LENGTH + EOL_LENGTH)
                .writeByte(RespMessageType.BULK_STRING.value()).writeShort(NULL_SHORT).writeShort(EOL_SHORT));
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.BULK_STRING;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public Integer toInteger() {
        return null;
    }

    @Override
    public Long toLong() {
        return null;
    }

    @Override
    public Double toDouble() {
        return null;
    }

    @Override
    public String toText(Charset charset) {
        return null;
    }

    @Override
    public BigDecimal toBigDecimal() {
        return null;
    }

    @Override
    public BigInteger toBigInteger() {
        return null;
    }

    @Override
    public int intValue() {
        throw RespCodecUtil.NaN;
    }

    @Override
    public long longValue() {
        throw RespCodecUtil.NaN;
    }

    @Override
    public double doubleValue() {
        throw RespCodecUtil.NaN;
    }

    @Override
    public BigInteger bigIntegerValue() {
        return null;
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return null;
    }

    @Override
    public String textValue(Charset charset) {
        return null;
    }

    @Override
    public AsciiString asciiValue() {
        return null;
    }

    @Override
    public CachedNullMessage replace(ByteBuf content) {
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "null]";
    }

    @Override
    public ByteBuf content() {
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public int refCnt() {
        return Unpooled.EMPTY_BUFFER.refCnt();
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }

    @Override
    public CachedNullMessage copy() {
        return this;
    }

    @Override
    public CachedNullMessage duplicate() {
        return this;
    }

    @Override
    public CachedNullMessage retainedDuplicate() {
        return this;
    }

    @Override
    public CachedNullMessage retain() {
        return this;
    }

    @Override
    public CachedNullMessage retain(int increment) {
        return this;
    }

    @Override
    public CachedNullMessage touch() {
        return this;
    }

    @Override
    public CachedNullMessage touch(Object hint) {
        return this;
    }

}
