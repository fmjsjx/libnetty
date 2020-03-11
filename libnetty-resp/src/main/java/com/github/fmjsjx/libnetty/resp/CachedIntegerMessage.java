package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import io.netty.buffer.ByteBuf;

public class CachedIntegerMessage extends AbstractCachedRespMessage<CachedIntegerMessage>
        implements RespIntegerMessage {

    public static final CachedIntegerMessage create(long value) {
        byte[] bytes = RespCodecUtil.longToAsciiBytes(value);
        int length = bytes.length;
        ByteBuf fullContent = fixedBuffer(length).writeBytes(RespMessageType.INTEGER.content()).writeBytes(bytes)
                .writeShort(EOL_SHORT).asReadOnly();
        ByteBuf content = fullContent.slice(fullContent.readerIndex() + TYPE_LENGTH, length);
        return new CachedIntegerMessage(content, fullContent, value);
    }

    private final long value;

    private CachedIntegerMessage(ByteBuf content, ByteBuf fullContent, long value) {
        super(content, fullContent);
        this.value = value;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.INTEGER;
    }

    @Override
    public CachedIntegerMessage replace(ByteBuf content) {
        return new CachedIntegerMessage(content, fullContent, value);
    }

    public long value() {
        return value;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
