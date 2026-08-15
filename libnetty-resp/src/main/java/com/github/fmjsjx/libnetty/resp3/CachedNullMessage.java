package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * A cached implementation of {@link Resp3NullMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedNullMessage extends CachedRespMessage implements Resp3NullMessage {

    static final CachedNullMessage INSTANCE = new CachedNullMessage();

    /**
     * Returns the singleton {@link CachedNullMessage} instance.
     * 
     * @return the singleton {@link CachedNullMessage} instance
     */
    public static final CachedNullMessage getInstance() {
        return INSTANCE;
    }

    private CachedNullMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + EOL_LENGTH).writeByte(Resp3MessageType.NULL.value())
                .writeShort(EOL_SHORT));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "null]";
    }

}
