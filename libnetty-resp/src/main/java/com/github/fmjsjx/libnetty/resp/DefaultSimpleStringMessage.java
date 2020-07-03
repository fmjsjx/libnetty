package com.github.fmjsjx.libnetty.resp;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;

/**
 * The default implementation of {@link RespSimpleStringMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultSimpleStringMessage extends AbstractContentRespMessage<DefaultSimpleStringMessage>
        implements RespSimpleStringMessage {

    /**
     * Creates a new {@link DefaultSimpleStringMessage} with the specified
     * {@code value} encoded in {@code UTF-8} character set.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the value encoded in {@code UTF-8}
     * @return a {@code DefaultSimpleStringMessage}
     */
    public static final DefaultSimpleStringMessage createUtf8(ByteBufAllocator alloc, CharSequence value) {
        return new DefaultSimpleStringMessage(ByteBufUtil.writeUtf8(alloc, value), value.toString(), CharsetUtil.UTF_8);
    }

    /**
     * Creates a new {@link DefaultSimpleStringMessage} with the specified
     * {@code value} encoded in {@code US-ASCII} character set.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the value encoded in {@code US-ASCII}
     * @return a {@code DefaultSimpleStringMessage}
     */
    public static final DefaultSimpleStringMessage createAscii(ByteBufAllocator alloc, CharSequence value) {
        return new DefaultSimpleStringMessage(ByteBufUtil.writeAscii(alloc, value), value.toString(),
                CharsetUtil.US_ASCII);
    }

    /**
     * Creates a new {@link DefaultSimpleStringMessage} with the specified
     * {@code value} encoded in the specified {@link Charset}.
     * 
     * @param alloc   the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value   the value
     * @param charset the {@code Charset} of the value
     * @return a {@code DefaultSimpleStringMessage}
     */
    public static final DefaultSimpleStringMessage create(ByteBufAllocator alloc, CharSequence value, Charset charset) {
        return new DefaultSimpleStringMessage(ByteBufUtil.encodeString(alloc, CharBuffer.wrap(value), charset),
                value.toString(), charset);
    }

    private final String value;
    private final Charset charset;

    private DefaultSimpleStringMessage(ByteBuf content, String value, Charset charset) {
        super(content);
        this.value = value;
        this.charset = charset;
    }
    
    DefaultSimpleStringMessage(ByteBuf content, Charset charset) {
        this(content, content.toString(charset), charset);
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.SIMPLE_STRING;
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        out.add(type().content());
        out.add(content.retain());
        out.add(RespConstants.EOL_BUF.duplicate());
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public DefaultSimpleStringMessage replace(ByteBuf content) {
        return new DefaultSimpleStringMessage(content, value, charset);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
