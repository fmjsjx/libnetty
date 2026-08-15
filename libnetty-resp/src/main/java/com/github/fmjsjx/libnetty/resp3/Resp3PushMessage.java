package com.github.fmjsjx.libnetty.resp3;

import java.util.Collections;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.AbstractRespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

/**
 * An interface defines a RESP3 Push message.
 * 
 * @param <E> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3PushMessage<E extends RespMessage> extends RespAggregateMessage<E> {

    /**
     * Returns an unmodifiable {@link Resp3PushMessage} containing a single value.
     * 
     * @param <E>   the type of values in the message
     * @param value the value
     * @return an unmodifiable {@link Resp3PushMessage} containing a single value
     */
    static <E extends RespMessage> Resp3PushMessage<E> of(E value) {
        return new ImmutablePush1Message<>(value);
    }

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.PUSH;
    }

}

class ImmutablePush1Message<E extends RespMessage> extends AbstractRespAggregateMessage<E, ImmutablePush1Message<E>>
        implements Resp3PushMessage<E> {

    private final E value;
    private final List<E> values;

    ImmutablePush1Message(E value) {
        this.value = value;
        this.values = Collections.singletonList(value);
    }

    @Override
    public List<E> values() {
        return values;
    }

    @Override
    public ImmutablePush1Message<E> touch(Object hint) {
        ReferenceCountUtil.touch(value, hint);
        return this;
    }

    @Override
    protected void encodeValue(ByteBufAllocator alloc, E value, List<Object> out) throws Exception {
        value.encode(alloc, out);
    }

    @Override
    protected void deallocate() {
        ReferenceCountUtil.release(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(1)[" + value + "]]";
    }

}