package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;
import static com.github.fmjsjx.libnetty.resp3.Resp3MessageType.MAP;

import java.util.Collections;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;
import com.github.fmjsjx.libnetty.resp.RespMessage;

/**
 * The cached implementation of {@link Resp3MapMessage} for empty map.
 * 
 * @param <F> the type of fields in this map message
 * @param <V> the type of values in this map message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedEmptyMapMessage<F extends RespMessage, V extends RespMessage> extends CachedRespMessage
        implements Resp3MapMessage<F, V> {

    private static final CachedEmptyMapMessage<? extends RespMessage, ? extends RespMessage> instance = new CachedEmptyMapMessage<>();

    /**
     * Returns the singleton {@link CachedEmptyMapMessage} instance.
     * 
     * @param <F> the type of fields in this map message
     * @param <V> the type of values in this map message
     * 
     * @return the singleton {@link CachedEmptyMapMessage} instance
     */
    @SuppressWarnings("unchecked")
    public static final <F extends RespMessage, V extends RespMessage> CachedEmptyMapMessage<F, V> getInstance() {
        return (CachedEmptyMapMessage<F, V>) instance;
    }

    private CachedEmptyMapMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(MAP.value()).writeByte('0')
                .writeShort(EOL_SHORT));
    }

    @Override
    public List<FieldValuePair<F, V>> values() {
        return Collections.emptyList();
    }

    @Override
    public int refCnt() {
        return fullContent.refCnt();
    }

    @Override
    public CachedEmptyMapMessage<F, V> retain() {
        return this;
    }

    @Override
    public CachedEmptyMapMessage<F, V> retain(int increment) {
        return this;
    }

    @Override
    public CachedEmptyMapMessage<F, V> touch() {
        fullContent.touch();
        return this;
    }

    @Override
    public CachedEmptyMapMessage<F, V> touch(Object hint) {
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
