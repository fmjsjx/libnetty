package com.github.fmjsjx.libnetty.resp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.AsciiString;

class RespConstants {

    static final int TYPE_LENGTH = 1;

    static final int EOL_LENGTH = 2;

    static final int NULL_LENGTH = 2;

    static final int NULL_VALUE = -1;

    static final int RESP_MESSAGE_MAX_LENGTH = 512 * 1024 * 1024; // 512MB

    // 64KB is max inline length of current Redis server implementation.
    static final int RESP_INLINE_MESSAGE_MAX_LENGTH = 64 * 1024;

    static final int POSITIVE_LONG_MAX_LENGTH = 19; // length of Long.MAX_VALUE

    static final int POSITIVE_INT_MAX_LENGTH = 10; // length of Integer.MAX_VALUE

    static final int LONG_MAX_LENGTH = POSITIVE_LONG_MAX_LENGTH + 1; // +1 is sign

    static final int INT_MAX_LENGTH = POSITIVE_INT_MAX_LENGTH + 1; // +1 is sign

    static final short NULL_SHORT = RespCodecUtil.makeShort('-', '1');

    static final short EOL_SHORT = RespCodecUtil.makeShort('\r', '\n');

    static final ByteBuf EOL_BUF = Unpooled.unreleasableBuffer(
            UnpooledByteBufAllocator.DEFAULT.buffer(EOL_LENGTH, EOL_LENGTH).writeShort(EOL_SHORT).asReadOnly());

    static final byte TYPE_ARRAY = '*';
    static final byte TYPE_BULK_STRING = '$';
    static final byte TYPE_SIMPLE_STRING = '+';
    static final byte TYPE_ERROR = '-';
    static final byte TYPE_INTEGER = ':';

    public static final AsciiString ERR = AsciiString.cached("ERR");

    private RespConstants() {
    }

}
