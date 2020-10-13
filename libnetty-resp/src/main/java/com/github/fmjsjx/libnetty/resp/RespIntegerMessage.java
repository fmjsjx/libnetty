package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Integer message.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespIntegerMessage extends RespMessage {
    
    @Override
    default RespMessageType type() {
        return RespMessageType.INTEGER;
    }

    /**
     * Returns the value.
     * 
     * @return the value
     */
    long value();

}
