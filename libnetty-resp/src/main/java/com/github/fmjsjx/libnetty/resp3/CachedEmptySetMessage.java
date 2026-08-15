package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;
import static com.github.fmjsjx.libnetty.resp3.Resp3MessageType.SET;

import java.util.Collections;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;
import com.github.fmjsjx.libnetty.resp.RespMessage;

/**
 * The cached implementation of {@link Resp3SetMessage} for empty set.
 * 
 * @param <E> the type of values in message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedEmptySetMessage<E extends RespMessage> extends CachedRespMessage implements Resp3SetMessage<E> {

    private static final CachedEmptySetMessage<? extends RespMessage> INSTANCE = new CachedEmptySetMessage<>();

    /**
     * Returns the singleton {@link CachedEmptySetMessage} instance.
     * 
     * @param <E> the type of values in message
     * @return the singleton {@link CachedEmptySetMessage} instance
     */
    @SuppressWarnings("unchecked")
    public static final <E extends RespMessage> CachedEmptySetMessage<E> getInstance() {
        return (CachedEmptySetMessage<E>) INSTANCE;
    }

    private CachedEmptySetMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(SET.value()).writeByte('0')
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
    public CachedEmptySetMessage<E> retain() {
        fullContent.retain();
        return this;
    }

    @Override
    public CachedEmptySetMessage<E> retain(int increment) {
        fullContent.retain(increment);
        return this;
    }

    @Override
    public CachedEmptySetMessage<E> touch() {
        fullContent.touch();
        return this;
    }

    @Override
    public CachedEmptySetMessage<E> touch(Object hint) {
        fullContent.touch(hint);
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
        return getClass().getSimpleName() + "[" + type() + "(0)]";
    }

}
