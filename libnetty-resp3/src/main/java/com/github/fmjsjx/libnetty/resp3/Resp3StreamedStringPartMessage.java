package com.github.fmjsjx.libnetty.resp3;

import com.github.fmjsjx.libnetty.resp.RespContent;

/**
 * An interface defines a RESP3 Streamed String Part message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3StreamedStringPartMessage extends Resp3Message, RespContent {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.STREAMED_STRING_PART;
    }

}
