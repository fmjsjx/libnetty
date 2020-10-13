package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

/**
 * The cached implementation of the {@link RespSimpleStringMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class CachedSimpleStringMessage extends CachedRespMessage implements RespSimpleStringMessage {

    /**
     * Returns a new cached {@link CachedSimpleStringMessage} with the specific
     * {@code message} and {@code US-ASCII} character set.
     * 
     * @param message the value message
     * @return a {@code CachedSimpleStringMessage}
     */
    public static final CachedSimpleStringMessage createAscii(CharSequence message) {
        return create(message, CharsetUtil.US_ASCII);
    }

    /**
     * Returns a new cached {@link CachedSimpleStringMessage} with the specific
     * {@code message} and {@code UTF-8} character set.
     * 
     * @param message the value message
     * @return a {@code CachedSimpleStringMessage}
     */
    public static final CachedSimpleStringMessage createUtf8(CharSequence message) {
        return create(message, CharsetUtil.UTF_8);
    }

    private static final CachedSimpleStringMessage create(CharSequence message, Charset charset) {
        String value = message.toString();
        byte[] bytes = value.getBytes(charset);
        ByteBuf fullContent = RespCodecUtil.buffer(TYPE_LENGTH + bytes.length + EOL_LENGTH)
                .writeBytes(RespMessageType.SIMPLE_STRING.content()).writeBytes(bytes).writeShort(EOL_SHORT)
                .asReadOnly();
        return new CachedSimpleStringMessage(fullContent, value);
    }

    private final String value;

    private CachedSimpleStringMessage(ByteBuf fullContent, String value) {
        super(fullContent);
        this.value = value;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.SIMPLE_STRING;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
