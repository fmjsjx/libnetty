package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.util.ReferenceCounted;

/**
 * An interface defines RESP aggregate message. The types described so far are
 * simple types that just define a single item of a given type. Combines the
 * {@link RespMessage} and the {@link ReferenceCounted}.
 * 
 * @param <E> the type of values in this message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface RespAggregateMessage<E extends RespObject> extends RespMessage, ReferenceCounted {

    /**
     * Returns the size of this message.
     * 
     * @return the size
     */
    int size();

    /**
     * Returns the values of this message.
     * 
     * @return the values
     */
    List<E> values();

}
