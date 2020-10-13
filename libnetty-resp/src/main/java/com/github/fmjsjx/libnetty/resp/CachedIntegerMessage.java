package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import io.netty.buffer.ByteBuf;

/**
 * The cached implementation of the {@link RespIntegerMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class CachedIntegerMessage extends CachedRespMessage implements RespIntegerMessage {

    /**
     * Returns a new {@link CachedIntegerMessage} with the specific {@code value}
     * given.
     * 
     * @param value the value
     * @return a {@code CachedIntegerMessage}
     */
    public static final CachedIntegerMessage create(long value) {
        byte[] bytes = RespCodecUtil.longToAsciiBytes(value);
        int length = bytes.length;
        ByteBuf fullContent = RespCodecUtil.buffer(TYPE_LENGTH + length + EOL_LENGTH)
                .writeBytes(RespMessageType.INTEGER.content()).writeBytes(bytes).writeShort(EOL_SHORT).asReadOnly();
        return new CachedIntegerMessage(fullContent, value);
    }

    private final long value;

    private CachedIntegerMessage(ByteBuf fullContent, long value) {
        super(fullContent);
        this.value = value;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.INTEGER;
    }

    @Override
    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
