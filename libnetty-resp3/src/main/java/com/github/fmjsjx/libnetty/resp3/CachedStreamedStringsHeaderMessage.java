package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;
import static com.github.fmjsjx.libnetty.resp3.Resp3MessageType.STREAMED_STRINGS_HEADER;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * The cached implementation of {@link Resp3StreamedStringsHeaderMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedStreamedStringsHeaderMessage extends CachedRespMessage implements Resp3StreamedStringsHeaderMessage {

    private static final CachedStreamedStringsHeaderMessage INSTANCE = new CachedStreamedStringsHeaderMessage();

    /**
     * Returns the singleton {@link CachedStreamedStringsHeaderMessage} instance.
     * 
     * @return the singleton {@link CachedStreamedStringsHeaderMessage} instance
     */
    public static final CachedStreamedStringsHeaderMessage getInstance() {
        return INSTANCE;
    }

    private CachedStreamedStringsHeaderMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(STREAMED_STRINGS_HEADER.value())
                .writeByte('?').writeShort(EOL_SHORT));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "]";
    }

}
