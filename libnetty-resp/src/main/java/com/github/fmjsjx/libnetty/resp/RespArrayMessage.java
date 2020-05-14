package com.github.fmjsjx.libnetty.resp;

import java.util.List;

/**
 * An interface defines a RESP Array message.
 *
 * @since 1.0
 * 
 * @author fmjsjx
 */
public interface RespArrayMessage extends RespMessage {

    /**
     * Returns the size of this {@link RespArrayMessage}.
     * 
     * @return the size
     */
    int size();

    /**
     * Returns the values of this {@link RespArrayMessage}.
     * 
     * @return a {@link List}
     */
    List<? extends RespMessage> values();

    /**
     * Returns the value at the specified position in this {@link RespArrayMessage}.
     * 
     * @param <M>   the type of the returned value
     * @param index index of the value to return
     * @return the value at the specified position
     */
    @SuppressWarnings("unchecked")
    default <M extends RespMessage> M value(int index) {
        return (M) values().get(index);
    }

    /**
     * Returns the value as {@link RespBulkStringMessage} at the specified position
     * in this {@link RespArrayMessage}.
     * 
     * @param index index of the value to return
     * @return a {@code RespBulkStringMessage}
     */
    default RespBulkStringMessage bulkString(int index) {
        return value(index);
    }

}
