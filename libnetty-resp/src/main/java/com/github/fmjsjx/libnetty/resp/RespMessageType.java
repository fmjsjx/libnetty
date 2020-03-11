package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

public class RespMessageType {

    public static final RespMessageType ARRAY = new RespMessageType(TYPE_ARRAY, false, "Array");
    public static final RespMessageType BULK_STRING = new RespMessageType(TYPE_BULK_STRING, false, "BulkString");
    public static final RespMessageType SIMPLE_STRING = new RespMessageType(TYPE_SIMPLE_STRING, true, "SimpleString");
    public static final RespMessageType ERROR = new RespMessageType(TYPE_ERROR, true, "Error");
    public static final RespMessageType INTEGER = new RespMessageType(TYPE_INTEGER, true, "Integer");
    public static final RespMessageType INLINE_COMMAND = new RespMessageType((byte) 0, true, "InlineCommand");

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

    private RespMessageType(byte value, boolean inline, String name) {
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

    public byte value() {
        return value;
    }

    public boolean isInlineType() {
        return inline;
    }

    public String text() {
        return text;
    }

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
