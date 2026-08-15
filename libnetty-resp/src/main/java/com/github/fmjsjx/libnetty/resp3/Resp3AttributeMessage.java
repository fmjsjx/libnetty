package com.github.fmjsjx.libnetty.resp3;

import java.util.Collections;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.AbstractRespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

/**
 * An interface defines a RESP3 Attribute message.
 * 
 * @param <F> the type of fields in the message
 * @param <V> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3AttributeMessage<F extends RespMessage, V extends RespMessage>
        extends RespAggregateMessage<FieldValuePair<F, V>> {

    /**
     * Returns an unmodifiable {@link Resp3AttributeMessage} containing a single
     * {@link FieldValuePair}.
     * 
     * @param <F>   the type of the field
     * @param <V>   the type of the value
     * @param field the field
     * @param value the value
     * @return an unmodifiable {@link Resp3AttributeMessage} containing a single
     *         {@link FieldValuePair}
     */
    static <F extends RespMessage, V extends RespMessage> Resp3AttributeMessage<F, V> of(F field, V value) {
        return new ImmutableAttribute1Message<>(field, value);
    }

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.ATTRIBUTE;
    }

}

class ImmutableAttribute1Message<F extends RespMessage, V extends RespMessage>
        extends AbstractRespAggregateMessage<FieldValuePair<F, V>, ImmutableAttribute1Message<F, V>>
        implements Resp3AttributeMessage<F, V> {

    private final F field;
    private final V value;
    private final List<FieldValuePair<F, V>> values;

    ImmutableAttribute1Message(F field, V value) {
        this.field = field;
        this.value = value;
        this.values = Collections.singletonList(new FieldValuePair<>(field, value));
    }

    @Override
    public List<FieldValuePair<F, V>> values() {
        return values;
    }

    @Override
    public ImmutableAttribute1Message<F, V> touch(Object hint) {
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