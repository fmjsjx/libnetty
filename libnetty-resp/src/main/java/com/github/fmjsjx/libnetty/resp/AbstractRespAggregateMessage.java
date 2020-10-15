package com.github.fmjsjx.libnetty.resp;

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

}
