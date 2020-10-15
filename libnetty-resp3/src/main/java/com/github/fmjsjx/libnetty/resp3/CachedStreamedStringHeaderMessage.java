package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;
import static com.github.fmjsjx.libnetty.resp3.Resp3MessageType.STREAMED_STRING_HEADER;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * The cached implementation of {@link Resp3StreamedStringHeaderMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedStreamedStringHeaderMessage extends CachedRespMessage implements Resp3StreamedStringHeaderMessage {

    private static final CachedStreamedStringHeaderMessage INSTANCE = new CachedStreamedStringHeaderMessage();

    /**
     * Returns the singleton {@link CachedStreamedStringHeaderMessage} instance.
     * 
     * @return the singleton {@link CachedStreamedStringHeaderMessage} instance
     */
    public static final CachedStreamedStringHeaderMessage getInstance() {
        return INSTANCE;
    }

    private CachedStreamedStringHeaderMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(STREAMED_STRING_HEADER.value())
                .writeByte('?').writeShort(EOL_SHORT));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "]";
    }

}
