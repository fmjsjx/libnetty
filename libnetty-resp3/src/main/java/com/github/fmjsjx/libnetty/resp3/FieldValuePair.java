package com.github.fmjsjx.libnetty.resp3;

import java.util.List;
import java.util.Objects;

import com.github.fmjsjx.libnetty.resp.RespMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * A container contains a pair of field and value.
 * 
 * @param <F> the type of the field, can by any {@link RespMessage}
 * @param <V> the type of the value, can by any {@link RespMessage}
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class FieldValuePair<F extends RespMessage, V extends RespMessage> {

    private final F field;
    private final V value;

    /**
     * Constructs a new {@link FieldValuePair} with the specified field and value.
     * 
     * @param field the field
     * @param value the value
     */
    public FieldValuePair(F field, V value) {
        this.field = Objects.requireNonNull(field, "field must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Encode this {@link FieldValuePair}.
     * 
     * @param alloc the {@link ByteBufAllocator} which will be used to allocate
     *              {@link ByteBuf}s
     * @param out   the {@link List} into which the encoded msg should be added
     * @throws Exception is thrown if an error occurs
     */
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        field.encode(alloc, out);
        value.encode(alloc, out);
    }

    /**
     * Returns the field.
     * 
     * @return the field
     */
    public F field() {
        return field;
    }

    /**
     * Returns the value.
     * 
     * @return the value
     */
    public V value() {
        return value;
    }

    @Override
    public String toString() {
        return field + "=" + value;
    }

}
