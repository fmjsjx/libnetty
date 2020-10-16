package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.util.Collections;
import java.util.List;

/**
 * The cached implementation of {@link RespArrayMessage} for empty array.
 * 
 * @param <E> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedEmptyArrayMessage<E extends RespMessage> extends CachedRespMessage implements RespArrayMessage<E> {

    private static final CachedEmptyArrayMessage<? extends RespMessage> INSTANCE = new CachedEmptyArrayMessage<>();

    /**
     * Returns the singleton {@link CachedEmptyArrayMessage} instance.
     * 
     * @param <E> the type of values in the message
     * @return the singleton {@link CachedEmptyArrayMessage} instance
     */
    @SuppressWarnings("unchecked")
    public static final <E extends RespMessage> CachedEmptyArrayMessage<E> getInstance() {
        return (CachedEmptyArrayMessage<E>) INSTANCE;
    }

    private CachedEmptyArrayMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(RespMessageType.ARRAY.value()).writeByte('0')
                .writeShort(EOL_SHORT));
    }

    @Override
    public List<E> values() {
        return Collections.emptyList();
    }

    @Override
    public int refCnt() {
        return fullContent.refCnt();
    }

    @Override
    public CachedEmptyArrayMessage<E> retain() {
        return this;
    }

    @Override
    public CachedEmptyArrayMessage<E> retain(int increment) {
        return this;
    }

    @Override
    public CachedEmptyArrayMessage<E> touch() {
        return this;
    }

    @Override
    public CachedEmptyArrayMessage<E> touch(Object hint) {
        return this;
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
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(0)[]]";
    }

}
