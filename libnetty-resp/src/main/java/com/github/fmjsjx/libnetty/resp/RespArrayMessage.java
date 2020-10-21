package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Array message.
 *
 * @param <E> the type the elements in this array
 *
 * @since 1.0
 * 
 * @author MJ Fang
 */
public interface RespArrayMessage<E extends RespMessage> extends RespAggregateMessage<E> {

    @Override
    default RespMessageType type() {
        return RespMessageType.ARRAY;
    }

    /**
     * Returns the value at the specified position in this {@link RespArrayMessage}.
     * 
     * @param index index of the value to return
     * @return the value at the specified position
     */
    default E value(int index) {
        return values().get(index);
    }

    /**
     * Returns the value as {@link RespBulkStringMessage} at the specified position
     * in this {@link RespArrayMessage}.
     * 
     * @param index index of the value to return
     * @return a {@code RespBulkStringMessage}
     */
    default RespBulkStringMessage bulkString(int index) {
        return RespBulkStringMessage.class.cast(value(index));
    }

}
