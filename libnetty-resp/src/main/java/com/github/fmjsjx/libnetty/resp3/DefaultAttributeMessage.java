package com.github.fmjsjx.libnetty.resp3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.fmjsjx.libnetty.resp.AbstractRespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

/**
 * The default implementation of {@link Resp3AttributeMessage}.
 * 
 * @param <F> the type of fields in the message
 * @param <V> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultAttributeMessage<F extends RespMessage, V extends RespMessage>
        extends AbstractRespAggregateMessage<FieldValuePair<F, V>, DefaultAttributeMessage<F, V>>
        implements Resp3AttributeMessage<F, V> {

    private final List<FieldValuePair<F, V>> values;

    /**
     * Constructs a new {@link DefaultAttributeMessage} instance with the specified
     * values.
     * 
     * @param values the values
     */
    @SafeVarargs
    public DefaultAttributeMessage(FieldValuePair<F, V>... values) {
        this(Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * Constructs a new {@link DefaultAttributeMessage} instance with the specified
     * values.
     * 
     * @param values the values
     */
    public DefaultAttributeMessage(Collection<FieldValuePair<F, V>> values) {
        this(new ArrayList<>(values));
    }

    /**
     * Constructs a new {@link DefaultAttributeMessage} instance.
     */
    public DefaultAttributeMessage() {
        this(new ArrayList<>());
    }

    DefaultAttributeMessage(List<FieldValuePair<F, V>> values) {
        this.values = values;
    }

    @Override
    public List<FieldValuePair<F, V>> values() {
        return values;
    }

    @Override
    protected void encodeValue(ByteBufAllocator alloc, FieldValuePair<F, V> value, List<Object> out) throws Exception {
        value.encode(alloc, out);
    }

    @Override
    public DefaultAttributeMessage<F, V> touch(Object hint) {
        for (FieldValuePair<F, V> pair : values()) {
            ReferenceCountUtil.touch(pair.field(), hint);
            ReferenceCountUtil.touch(pair.value(), hint);
        }
        return this;
    }

    @Override
    protected void deallocate() {
        for (FieldValuePair<F, V> pair : values()) {
            ReferenceCountUtil.release(pair.field());
            ReferenceCountUtil.release(pair.value());
        }
    }

    /**
     * Add a new {@link FieldValuePair} into this map with the specified field and
     * value.
     * 
     * @param field the field
     * @param value the value
     */
    public void put(F field, V value) {
        values.add(new FieldValuePair<>(field, value));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(" + size() + "){"
                + values().stream().map(Object::toString).collect(Collectors.joining(",")) + "}]";
    }

}
