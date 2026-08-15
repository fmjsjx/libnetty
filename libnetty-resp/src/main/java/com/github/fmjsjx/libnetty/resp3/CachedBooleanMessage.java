package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.FALSE_VALUE;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TRUE_VALUE;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * The cached implementation of {@link Resp3BooleanMessage}.
 *
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedBooleanMessage extends CachedRespMessage implements Resp3BooleanMessage {

    /**
     * The singleton {@link CachedBooleanMessage} instance: {@code True}.
     */
    public static final CachedBooleanMessage TRUE = new CachedBooleanMessage(true);
    /**
     * The singleton {@link CachedBooleanMessage} instance: {@code False}.
     */
    public static final CachedBooleanMessage FALSE = new CachedBooleanMessage(false);

    /**
     * Returns the singleton {@link CachedBooleanMessage} with the specified boolean
     * value.
     * 
     * @param value the value
     * @return the singleton {@link CachedBooleanMessage} with the specified boolean
     *         value
     */
    public static final CachedBooleanMessage valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

    private final boolean value;

    private CachedBooleanMessage(boolean value) {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(Resp3MessageType.BOOLEAN.value())
                .writeByte(value ? TRUE_VALUE : FALSE_VALUE).writeShort(EOL_SHORT));
        this.value = value;
    }

    @Override
    public boolean value() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
