package com.github.fmjsjx.libnetty.resp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;

/**
 * Constants of RESP.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class RespConstants {

    /**
     * Length of type: {@code 1}
     */
    public static final int TYPE_LENGTH = 1;

    /**
     * Length of EOL: {@code 2}
     */
    public static final int EOL_LENGTH = 2;

    /**
     * Length of null: {@code 2}
     */
    public static final int NULL_LENGTH = 2;

    /**
     * Value of null: {@code -1}
     */
    public static final int NULL_VALUE = -1;

    /**
     * Maximum length of RESP message: {@code 512MB}
     */
    public static final int RESP_MESSAGE_MAX_LENGTH = 512 * 1024 * 1024;

    /**
     * Maximum length of RESP in-line message.
     * <p>
     * 64KB is max inline length of current Redis server implementation.
     */
    public static final int RESP_INLINE_MESSAGE_MAX_LENGTH = 64 * 1024;

    /**
     * Maximum length of positive long: {@code 19}
     */
    public static final int POSITIVE_LONG_MAX_LENGTH = 19;

    /**
     * Maximum length of positive int: {@code 10}
     */
    public static final int POSITIVE_INT_MAX_LENGTH = 10;

    /**
     * Maximum length of long: {@code 20}
     */
    public static final int LONG_MAX_LENGTH = POSITIVE_LONG_MAX_LENGTH + 1; // +1 is sign

    /**
     * Maximum length of int: {@code 11}
     */
    public static final int INT_MAX_LENGTH = POSITIVE_INT_MAX_LENGTH + 1; // +1 is sign

    /**
     * Short value of null.
     */
    public static final short NULL_SHORT = RespCodecUtil.makeShort('-', '1');

    /**
     * Short value of EOL.
     */
    public static final short EOL_SHORT = RespCodecUtil.makeShort('\r', '\n');

    /**
     * {@link ByteBuf} contains EOL value.
     * <p>
     * The {@link ByteBuf} is unreleasable and read-only.
     */
    public static final ByteBuf EOL_BUF = Unpooled
            .unreleasableBuffer(RespCodecUtil.buffer(EOL_LENGTH).writeShort(EOL_SHORT).asReadOnly());

    /**
     * Sign of type array: {@code *}
     */
    public static final byte TYPE_ARRAY = '*';
    /**
     * Sign of type bulk string: {@code $}
     */
    public static final byte TYPE_BULK_STRING = '$';
    /**
     * Sign of type simple string: {@code +}
     */
    public static final byte TYPE_SIMPLE_STRING = '+';
    /**
     * Sign of type error: {@code -}
     */
    public static final byte TYPE_ERROR = '-';
    /**
     * Sign of type integer: {@code :}
     */
    public static final byte TYPE_INTEGER = ':';

    /**
     * ASCII value of space.
     */
    public static final byte SPACE = ' ';

    /**
     * {@code "ERR"}
     */
    public static final AsciiString ERR = AsciiString.cached("ERR");

    private RespConstants() {
    }

}
