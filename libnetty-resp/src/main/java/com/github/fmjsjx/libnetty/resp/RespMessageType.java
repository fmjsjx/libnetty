package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * The message type of RESP.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class RespMessageType {

    /**
     * RESP {@code Array}.
     */
    public static final RespMessageType ARRAY = new RespMessageType(TYPE_ARRAY, false, "Array");
    /**
     * RESP {@code Bulk String}.
     */
    public static final RespMessageType BULK_STRING = new RespMessageType(TYPE_BULK_STRING, false, "BulkString");
    /**
     * RESP {@code Simple String}.
     */
    public static final RespMessageType SIMPLE_STRING = new RespMessageType(TYPE_SIMPLE_STRING, true, "SimpleString");
    /**
     * RESP {@code Error}.
     */
    public static final RespMessageType ERROR = new RespMessageType(TYPE_ERROR, true, "Error");
    /**
     * RESP {@code Integer}
     */
    public static final RespMessageType INTEGER = new RespMessageType(TYPE_INTEGER, true, "Integer");
    /**
     * RESP {@code In-Line Command}
     */
    public static final RespMessageType INLINE_COMMAND = new RespMessageType((byte) 0, true, "InlineCommand");

    /**
     * Returns a {@link RespMessageType} instance by the sign value given.
     * 
     * @param value the sign value of the type
     * @return a {@code RespMessageType}
     */
    public static final RespMessageType valueOf(byte value) {
        switch (value) {
        case TYPE_ARRAY:
            return ARRAY;
        case TYPE_BULK_STRING:
            return BULK_STRING;
        case TYPE_SIMPLE_STRING:
            return SIMPLE_STRING;
        case TYPE_ERROR:
            return ERROR;
        case TYPE_INTEGER:
            return INTEGER;
        default:
            return INLINE_COMMAND;
        }
    }

    private final byte value;
    private final boolean inline;
    private final String text;
    private final ByteBuf content;

    protected RespMessageType(byte value, boolean inline, String name) {
        this.value = value;
        this.inline = inline;
        if (value == 0) {
            this.content = Unpooled.EMPTY_BUFFER;
            this.text = "(" + name + ")";
        } else {
            this.text = ((char) value) + "(" + name + ")";
            this.content = Unpooled
                    .unreleasableBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(1, 1).writeByte(value).asReadOnly());
        }
    }

    protected RespMessageType(char signChar, boolean inline, String name) {
        this((byte) signChar, inline, name);
    }

    /**
     * Returns the sign value.
     * 
     * @return the sign value
     */
    public byte value() {
        return value;
    }

    /**
     * Returns {@code true} if and only if this type is an {@code in-line} type.
     * 
     * @return {@code true} if is {@code in-line} type
     */
    public boolean isInlineType() {
        return inline;
    }

    /**
     * Returns the display text.
     * 
     * @return a {@code String}
     */
    public String text() {
        return text;
    }

    /**
     * Returns a cached {@link ByteBuf} content of this type.
     * 
     * @return a {@code ByteBuf}
     */
    ByteBuf content() {
        return content.duplicate();
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public String toString() {
        return text();
    }

}
