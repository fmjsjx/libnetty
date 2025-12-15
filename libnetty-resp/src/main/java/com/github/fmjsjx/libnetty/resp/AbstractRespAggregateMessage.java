package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AbstractReferenceCounted;

/**
 * The abstract implementation of {@link RespAggregateMessage}.
 * 
 * @param <E>    the type of values in this message
 * @param <Self> the type of the real class
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@SuppressWarnings("unchecked")
public abstract class AbstractRespAggregateMessage<E extends RespObject, Self extends AbstractRespAggregateMessage<E, ?>>
        extends AbstractReferenceCounted implements RespAggregateMessage<E> {

    /**
     * Constructs a new {@link AbstractRespAggregateMessage} instance.
     */
    protected AbstractRespAggregateMessage() {
    }

    @Override
    public Self retain() {
        super.retain();
        return (Self) this;
    }

    @Override
    public Self retain(int increment) {
        super.retain(increment);
        return (Self) this;
    }

    @Override
    public Self touch() {
        super.touch();
        return (Self) this;
    }

    @Override
    public abstract Self touch(Object hint);

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        encodeHeader(alloc, out);
        // encode values
        for (E e : values()) {
            encodeValue(alloc, e, out);
        }
    }

    protected void encodeHeader(ByteBufAllocator alloc, List<Object> out) {
        byte[] sizeBytes = RespCodecUtil.longToAsciiBytes(size());
        ByteBuf header = alloc.buffer(TYPE_LENGTH + sizeBytes.length + EOL_LENGTH).writeByte(type().value())
                .writeBytes(sizeBytes).writeShort(EOL_SHORT);
        out.add(header);
    }

    protected abstract void encodeValue(ByteBufAllocator alloc, E value, List<Object> out) throws Exception;

}
