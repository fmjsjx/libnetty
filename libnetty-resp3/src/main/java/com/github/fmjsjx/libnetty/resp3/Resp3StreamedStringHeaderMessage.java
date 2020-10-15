package com.github.fmjsjx.libnetty.resp3;

/**
 * An interface defines a RESP3 Streamed String Header message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3StreamedStringHeaderMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.STREAMED_STRING_HEADER;
    }

}
