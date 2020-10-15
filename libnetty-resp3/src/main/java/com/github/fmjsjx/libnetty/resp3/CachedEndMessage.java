package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;
import static com.github.fmjsjx.libnetty.resp3.Resp3MessageType.END;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * The cached implementation of {@link Resp3EndMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedEndMessage extends CachedRespMessage implements Resp3EndMessage {

    static final CachedEndMessage INSTANCE = new CachedEndMessage();

    /**
     * Returns the singleton {@link CachedEndMessage} instance.
     * 
     * @return the singleton {@link CachedEndMessage} instance
     */
    public static final CachedEndMessage getInstance() {
        return INSTANCE;
    }

    private CachedEndMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + EOL_LENGTH).writeByte(END.value()).writeShort(EOL_SHORT));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "]";
    }

}
