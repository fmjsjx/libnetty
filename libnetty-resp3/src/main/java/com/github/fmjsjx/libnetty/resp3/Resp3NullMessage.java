package com.github.fmjsjx.libnetty.resp3;

/**
 * An interface defines a RESP3 Null message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3NullMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.NULL;
    }

}
