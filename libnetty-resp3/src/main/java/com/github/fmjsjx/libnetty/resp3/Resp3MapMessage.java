package com.github.fmjsjx.libnetty.resp3;

import java.util.Collections;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.AbstractRespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

/**
 * An interface defines a RESP3 Map message.
 * 
 * @param <F> the type of fields in the message
 * @param <V> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3MapMessage<F extends RespMessage, V extends RespMessage>
        extends RespAggregateMessage<FieldValuePair<F, V>> {

    /**
     * Returns an unmodifiable empty {@link Resp3MapMessage}.
     * 
     * @param <F> the type of the field
     * @param <V> the type of the value
     * 
     * @return an unmodifiable empty {@link Resp3MapMessage}
     */
    static <F extends RespMessage, V extends RespMessage> Resp3MapMessage<F, V> of() {
        return CachedEmptyMapMessage.getInstance();
    }

    /**
     * Returns an unmodifiable {@link Resp3MapMessage} containing a single
     * {@link FieldValuePair}.
     * 
     * @param <F>   the type of the field
     * @param <V>   the type of the value
     * @param field the field
     * @param value the value
     * @return an unmodifiable {@link Resp3MapMessage} containing a single
     *         {@link FieldValuePair}
     */
    static <F extends RespMessage, V extends RespMessage> Resp3MapMessage<F, V> of(F field, V value) {
        return new ImmutableMap1Message<>(field, value);
    }

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.MAP;
    }

}

class ImmutableMap1Message<F extends RespMessage, V extends RespMessage>
        extends AbstractRespAggregateMessage<FieldValuePair<F, V>, ImmutableMap1Message<F, V>>
        implements Resp3MapMessage<F, V> {

    private final F field;
    private final V value;
    private final List<FieldValuePair<F, V>> values;

    ImmutableMap1Message(F field, V value) {
        this.field = field;
        this.value = value;
        this.values = Collections.singletonList(new FieldValuePair<>(field, value));
    }

    @Override
    public List<FieldValuePair<F, V>> values() {
        return values;
    }

    @Override
    public ImmutableMap1Message<F, V> touch(Object hint) {
        ReferenceCountUtil.touch(field, hint);
        ReferenceCountUtil.touch(value, hint);
        return this;
    }

    @Override
    protected void encodeValue(ByteBufAllocator alloc, FieldValuePair<F, V> value, List<Object> out) throws Exception {
        value.encode(alloc, out);
    }

    @Override
    protected void deallocate() {
        ReferenceCountUtil.release(field);
        ReferenceCountUtil.release(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(1){" + values().get(0) + "}]";
    }

}