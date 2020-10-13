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
public class CachedResp3NullMessage extends CachedRespMessage implements Resp3NullMessage {

    static final CachedResp3NullMessage INSTANCE = new CachedResp3NullMessage();

    /**
     * Returns the singleton {@link CachedResp3NullMessage} instance.
     * 
     * @return the singleton {@link CachedResp3NullMessage} instance
     */
    public static final CachedResp3NullMessage getInstance() {
        return INSTANCE;
    }

    private CachedResp3NullMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + EOL_LENGTH).writeByte(Resp3MessageType.NULL.value())
                .writeShort(EOL_SHORT));
    }

}
