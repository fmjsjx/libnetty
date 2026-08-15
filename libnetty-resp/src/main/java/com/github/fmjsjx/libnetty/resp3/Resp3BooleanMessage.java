package com.github.fmjsjx.libnetty.resp3;

/**
 * An interface defines a RESP3 Boolean message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3BooleanMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.BOOLEAN;
    }

    /**
     * Returns the boolean value
     * 
     * @return the boolean value
     */
    boolean value();

}
