package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * An implementation of {@link RespBulkStringMessage} never holding a
 * {@link ByteBuf}.
 * 
 * @since 1.2
 *
 * @author MJ Fang
 */
public class HeapBulkStringMessage implements RespBulkStringMessage {

    private final String value;

    /**
     * Constructs a new {@link HeapBulkStringMessage} with the specified value.
     * 
     * @param value the value
     */
    public HeapBulkStringMessage(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        byte[] b = value.getBytes(CharsetUtil.UTF_8);
        byte[] lengthBytes = RespCodecUtil.longToAsciiBytes(b.length);
        ByteBuf content = alloc.buffer(TYPE_LENGTH + lengthBytes.length + EOL_LENGTH + b.length + EOL_LENGTH);
        content.writeByte(type().value()).writeBytes(lengthBytes).writeShort(EOL_SHORT).writeBytes(b)
                .writeShort(EOL_SHORT);
        out.add(content);
    }

    @Override
    public HeapBulkStringMessage copy() {
        return new HeapBulkStringMessage(value);
    }

    @Override
    public HeapBulkStringMessage duplicate() {
        return this;
    }

    @Override
    public HeapBulkStringMessage retainedDuplicate() {
        return this;
    }

    @Override
    public HeapBulkStringMessage replace(ByteBuf content) {
        return new HeapBulkStringMessage(content.toString(CharsetUtil.UTF_8));
    }

    @Override
    public HeapBulkStringMessage retain() {
        return this;
    }

    @Override
    public HeapBulkStringMessage retain(int increment) {
        return this;
    }

    @Override
    public HeapBulkStringMessage touch() {
        return this;
    }

    @Override
    public HeapBulkStringMessage touch(Object hint) {
        return this;
    }

    @Override
    public ByteBuf content() {
        return Unpooled.wrappedBuffer(value.getBytes(CharsetUtil.UTF_8));
    }

    @Override
    public int refCnt() {
        return 0;
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
    public boolean isNull() {
        return false;
    }

    @Override
    public int intValue() {
        return Integer.parseInt(value);
    }

    @Override
    public long longValue() {
        return Long.parseLong(value);
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(value);
    }

    @Override
    public BigInteger bigIntegerValue() {
        return new BigInteger(value);
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(value);
    }

    @Override
    public String textValue(Charset charset) {
        return value;
    }

    @Override
    public AsciiString asciiValue() {
        return AsciiString.cached(value);
    }

    @Override
    public String toText(Charset charset) {
        return textValue(charset);
    }

    @Override
    public Integer toInteger() {
        return Integer.valueOf(value);
    }

    @Override
    public Long toLong() {
        return Long.valueOf(value);
    }

    @Override
    public AsciiString toAscii() {
        return asciiValue();
    }

    @Override
    public String toString() {
        return "HeapBulkStringMessage[value=" + value + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HeapBulkStringMessage) {
            return value.equals(((HeapBulkStringMessage) obj).value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
