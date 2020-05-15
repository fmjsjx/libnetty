package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.NULL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.NULL_SHORT;

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
 * @author fmjsjx
 */
public class CachedNullMessage extends AbstractCachedRespMessage<CachedNullMessage> implements RespBulkStringMessage {

    static final CachedNullMessage instance = new CachedNullMessage();

    /**
     * Returns the <b>SINGLETON</b> {@link CachedNullMessage} instance.
     * 
     * @return a {@code CachedNullMessage}
     */
    public static final CachedNullMessage instance() {
        return instance;
    }

    private CachedNullMessage() {
        super(Unpooled.EMPTY_BUFFER, fixedBuffer(NULL_LENGTH).writeBytes(RespMessageType.BULK_STRING.content())
                .writeShort(NULL_SHORT).writeShort(EOL_SHORT));
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

}
