package com.github.fmjsjx.libnetty.resp3;

/**
 * An interface defines a RESP3 End message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3EndMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.END;
    }

}
