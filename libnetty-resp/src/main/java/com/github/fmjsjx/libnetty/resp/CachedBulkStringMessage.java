package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class CachedBulkStringMessage extends AbstractCachedRespMessage<CachedBulkStringMessage>
        implements RespBulkStringMessage {

    public static final CachedBulkStringMessage create(int value) {
        CachedBulkStringMessage bulkString = createAscii(Integer.toString(value));
        bulkString.cachedNumbers.put(Integer.class, value);
        return bulkString;
    }

    public static final CachedBulkStringMessage create(long value) {
        CachedBulkStringMessage bulkString = createAscii(Long.toString(value));
        bulkString.cachedNumbers.put(Long.class, value);
        return bulkString;
    }

    public static final CachedBulkStringMessage createAscii(CharSequence value) {
        return create(value, CharsetUtil.US_ASCII);
    }

    public static final CachedBulkStringMessage createUtf8(CharSequence value) {
        return create(value, CharsetUtil.UTF_8);
    }

    public static final CachedBulkStringMessage create(CharSequence value, Charset charset) {
        String text = value.toString();
        byte[] bytes = text.getBytes(charset);
        int length = bytes.length;
        byte[] lengthBytes = RespCodecUtil.longToAsciiBytes(length);
        ByteBuf fullContent = fixedBuffer(lengthBytes.length + EOL_LENGTH + length)
                .writeBytes(RespMessageType.BULK_STRING.content()).writeBytes(lengthBytes).writeShort(EOL_SHORT)
                .writeBytes(bytes).writeShort(EOL_SHORT).asReadOnly();
        ByteBuf content = length == 0 ? Unpooled.EMPTY_BUFFER
                : fullContent.slice(fullContent.readerIndex() + TYPE_LENGTH + lengthBytes.length + EOL_LENGTH, length);
        AsciiString asciiString = null;
        if (charset.equals(CharsetUtil.US_ASCII) || ByteBufUtil.isText(content, CharsetUtil.US_ASCII)) {
            asciiString = AsciiString.cached(text);
        }
        CachedBulkStringMessage bulkString = new CachedBulkStringMessage(content, fullContent, asciiString);
        bulkString.cachedTexts.put(charset, text);
        return bulkString;
    }

    private final AsciiString asciiValue;
    private final ConcurrentMap<Class<? extends Number>, Number> cachedNumbers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Charset, String> cachedTexts = new ConcurrentHashMap<>();

    private CachedBulkStringMessage(ByteBuf content, ByteBuf fullContent, AsciiString asciiValue) {
        super(content, fullContent);
        this.asciiValue = asciiValue;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public int intValue() {
        return cachedNumbers.computeIfAbsent(Integer.class, k -> toInteger()).intValue();
    }

    @Override
    public long longValue() {
        return cachedNumbers.computeIfAbsent(Long.class, k -> toLong()).longValue();
    }

    @Override
    public double doubleValue() {
        return cachedNumbers.computeIfAbsent(Double.class, k -> toDouble()).doubleValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return (BigInteger) cachedNumbers.computeIfAbsent(BigInteger.class, k -> toBigInteger());
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return (BigDecimal) cachedNumbers.computeIfAbsent(BigDecimal.class, k -> toBigDecimal());
    }

    @Override
    public String textValue(Charset charset) {
        return cachedTexts.computeIfAbsent(charset, super::toText);
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.BULK_STRING;
    }

    @Override
    public AsciiString asciiValue() {
        return asciiValue;
    }

    @Override
    public CachedBulkStringMessage replace(ByteBuf content) {
        return new CachedBulkStringMessage(content, fullContent, asciiValue);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + textValue(CharsetUtil.UTF_8) + "]";
    }

}
