package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * The default implementation of {@link RespIntegerMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultIntegerMessage extends AbstractContentRespMessage<DefaultIntegerMessage>
        implements RespIntegerMessage {

    /**
     * Creates a new {@link DefaultIntegerMessage} with the specified {@code value}.
     * 
     * @param alloc the {@link ByteBufAllocator} to allocate {@link ByteBuf}s
     * @param value the long value
     * @return a {@code DefaultIntegerMessage}
     */
    public static final DefaultIntegerMessage create(ByteBufAllocator alloc, long value) {
        byte[] bytes = RespCodecUtil.longToAsciiBytes(value);
        ByteBuf content = alloc.buffer(bytes.length).writeBytes(bytes);
        return new DefaultIntegerMessage(content, value);
    }

    private final long value;

    DefaultIntegerMessage(ByteBuf content, long value) {
        super(content);
        this.value = value;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.INTEGER;
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        out.add(type().content());
        out.add(content.retain());
        out.add(RespConstants.EOL_BUF.duplicate());
    }

    @Override
    public long value() {
        return value;
    }

    @Override
    public DefaultIntegerMessage replace(ByteBuf content) {
        return new DefaultIntegerMessage(content, value);
    }

}
