package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * The default implementation of {@link RespBulkStringMessage}.
 * 
 * @since 1.0
 *
 * @author fmjsjx
 */
public class DefaultBulkStringMessage extends AbstractContentRespMessage<DefaultBulkStringMessage>
        implements RespBulkStringMessage {

    /**
     * Creates a new {@link DefaultBulkStringMessage} with the specified value
     * encoded in {@code UTF-8} character set.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the value encoded in {@code UTF-8}
     * @return a {@code DefaultBulkStringMessage}
     */
    public static final DefaultBulkStringMessage createUtf8(ByteBufAllocator alloc, CharSequence value) {
        return new DefaultBulkStringMessage(ByteBufUtil.writeUtf8(alloc, value), null, value.toString(),
                CharsetUtil.UTF_8, null);
    }

    /**
     * Creates a new {@link DefaultBulkStringMessage} with the specified value
     * encoded in {@code UTF-8} character set.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the value encoded in {@code US-ASCII}
     * @return a {@code DefaultBulkStringMessage}
     */
    public static final DefaultBulkStringMessage createAscii(ByteBufAllocator alloc, CharSequence value) {
        return new DefaultBulkStringMessage(ByteBufUtil.writeAscii(alloc, value), null, value.toString(),
                CharsetUtil.US_ASCII, AsciiString.of(value));
    }

    /**
     * Creates a new {@link DefaultBulkStringMessage} with the specified value
     * encoded in specified {@link Charset}.
     * 
     * @param alloc   the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value   the value
     * @param charset the {@code Charset} of the value
     * @return a {@code DefaultBulkStringMessage}
     */
    public static final DefaultBulkStringMessage create(ByteBufAllocator alloc, CharSequence value, Charset charset) {
        if (charset.equals(CharsetUtil.UTF_8)) {
            return createUtf8(alloc, value);
        } else if (charset.equals(CharsetUtil.US_ASCII)) {
            return createAscii(alloc, value);
        }
        return new DefaultBulkStringMessage(ByteBufUtil.encodeString(alloc, CharBuffer.wrap(value), charset), null,
                value.toString(), charset, null);
    }

    /**
     * Creates a new {@link DefaultBulkStringMessage} with the specified integer
     * value.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the integer value
     * @return a {@code DefaultBulkStringMessage}
     */
    public static final DefaultBulkStringMessage create(ByteBufAllocator alloc, int value) {
        byte[] bytes = RespCodecUtil.longToAsciiBytes(value);
        ByteBuf content = alloc.buffer(bytes.length);
        AsciiString ascii = new AsciiString(bytes, false);
        return new DefaultBulkStringMessage(content, Integer.valueOf(value), null, null, ascii);
    }

    /**
     * Creates a new {@link DefaultBulkStringMessage} with the specified long value.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the long value
     * @return a {@code DefaultBulkStringMessage}
     */
    public static final DefaultBulkStringMessage create(ByteBufAllocator alloc, long value) {
        byte[] bytes = RespCodecUtil.longToAsciiBytes(value);
        ByteBuf content = alloc.buffer(bytes.length);
        AsciiString ascii = new AsciiString(bytes, false);
        return new DefaultBulkStringMessage(content, Long.valueOf(value), null, null, ascii);
    }

    /**
     * Creates a new {@link DefaultBulkStringMessage} with the specified double
     * value.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the double value
     * @return a {@code DefaultBulkStringMessage}
     */
    public static final DefaultBulkStringMessage create(ByteBufAllocator alloc, double value) {
        byte[] bytes = RespCodecUtil.doubleToAsciiBytes(value);
        ByteBuf content = alloc.buffer(bytes.length);
        AsciiString ascii = new AsciiString(bytes, false);
        return new DefaultBulkStringMessage(content, Double.valueOf(value), null, null, ascii);
    }

    private Number cachedNumber;
    private String cachedText;
    private Charset cachedCharset;
    private AsciiString cachedAscii;

    /**
     * Constructs a new {@link DefaultBulkStringMessage} with the specified
     * {@link ByteBuf} content.
     * 
     * @param content a {@code ByteBuf}
     */
    public DefaultBulkStringMessage(ByteBuf content) {
        super(content);
    }

    private DefaultBulkStringMessage(ByteBuf content, Number cachedNumber, String cachedText, Charset cachedCharset,
            AsciiString cachedAscii) {
        super(content);
        this.cachedNumber = cachedNumber;
        this.cachedText = cachedText;
        this.cachedCharset = cachedCharset;
        this.cachedAscii = cachedAscii;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.BULK_STRING;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        byte[] length = RespCodecUtil.longToAsciiBytes(content.readableBytes());
        ByteBuf lengthBuf = alloc.buffer(length.length + EOL_LENGTH).writeBytes(length).writeShort(EOL_SHORT);
        out.add(type().content()); // sign
        out.add(lengthBuf);// length
        out.add(content.retain()); // content
        out.add(EOL_BUF.duplicate());
    }

    @Override
    public DefaultBulkStringMessage replace(ByteBuf content) {
        return new DefaultBulkStringMessage(content, cachedNumber, cachedText, cachedCharset, cachedAscii);
    }

    @Override
    public int intValue() {
        if (cachedNumber == null || !(cachedNumber instanceof Integer)) {
            cachedNumber = toInteger();
        }
        return cachedNumber.intValue();
    }

    @Override
    public long longValue() {
        if (cachedNumber == null || !(cachedNumber instanceof Long) || !(cachedNumber instanceof Integer)) {
            cachedNumber = toLong();
        }
        return cachedNumber.longValue();
    }

    @Override
    public double doubleValue() {
        if (cachedNumber == null || !(cachedNumber instanceof Double)) {
            cachedNumber = toDouble();
        }
        return cachedNumber.doubleValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        if (cachedNumber == null) {
            cachedNumber = toBigInteger();
        } else if (cachedNumber instanceof Integer || cachedNumber instanceof Long) {
            return BigInteger.valueOf(cachedNumber.longValue());
        } else if (!(cachedNumber instanceof BigInteger)) {
            cachedNumber = toBigInteger();
        }
        return (BigInteger) cachedNumber;
    }

    @Override
    public BigDecimal bigDecimalValue() {
        if (cachedNumber == null) {
            cachedNumber = toBigInteger();
        } else if (cachedNumber instanceof Integer || cachedNumber instanceof Long) {
            return BigDecimal.valueOf(cachedNumber.longValue());
        } else if (cachedNumber instanceof Double) {
            return BigDecimal.valueOf(cachedNumber.doubleValue());
        } else if (!(cachedNumber instanceof BigDecimal)) {
            cachedNumber = toBigDecimal();
        }
        return (BigDecimal) cachedNumber;
    }

    @Override
    public String textValue(Charset charset) {
        if (cachedText == null || !charset.equals(cachedCharset)) {
            cachedText = toText(cachedCharset = charset);
        }
        return cachedText;
    }

    @Override
    public String textValue() {
        if (cachedText == null) {
            return textValue(CharsetUtil.UTF_8);
        }
        return cachedText;
    }

    @Override
    public AsciiString asciiValue() {
        if (cachedAscii == null) {
            cachedAscii = toAscii();
        }
        return cachedAscii;
    }

    /**
     * Returns the cached {@link Charset}.
     * 
     * @return a {@code Charset}.
     */
    public Charset charset() {
        return cachedCharset;
    }

}
