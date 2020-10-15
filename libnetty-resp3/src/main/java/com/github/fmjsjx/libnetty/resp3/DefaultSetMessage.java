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
 * The default implementation of {@link Resp3MessageType}.
 * 
 * @param <E> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultSetMessage<E extends RespMessage> extends AbstractRespAggregateMessage<E, DefaultSetMessage<E>>
        implements Resp3SetMessage<E> {

    private final List<E> values;

    /**
     * Constructs a new {@link DefaultSetMessage} with the specified values.
     * 
     * @param values the values
     */
    @SafeVarargs
    public DefaultSetMessage(E... values) {
        this(Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * Constructs a new {@link DefaultSetMessage} with the specified values.
     * 
     * @param values the values
     */
    public DefaultSetMessage(Collection<E> values) {
        this(new ArrayList<>(values));
    }

    /**
     * Constructs a new {@link DefaultSetMessage} with the specified values.
     * 
     * @param values the values
     */
    public DefaultSetMessage() {
        this(new ArrayList<>());
    }

    DefaultSetMessage(List<E> values) {
        this.values = values;
    }

    @Override
    public List<E> values() {
        return values;
    }

    @Override
    public DefaultSetMessage<E> touch(Object hint) {
        for (E value : values) {
            ReferenceCountUtil.touch(value, hint);
        }
        return this;
    }

    @Override
    protected void encodeValue(ByteBufAllocator alloc, E value, List<Object> out) throws Exception {
        value.encode(alloc, out);
    }

    @Override
    protected void deallocate() {
        for (E value : values) {
            ReferenceCountUtil.release(value);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(" + size() + ")" + values + "]";
    }

    /**
     * Appends the specified value to the end of this set.
     * 
     * @param value this value
     * @return {@code true}
     */
    public boolean add(E value) {
        return values.add(value);
    }

}
