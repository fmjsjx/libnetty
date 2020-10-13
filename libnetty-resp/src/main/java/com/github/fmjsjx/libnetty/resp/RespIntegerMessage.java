package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Integer message. Combines the {@link RespMessage}
 * and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespIntegerMessage extends RespMessage {

    /**
     * Returns the value.
     * 
     * @return the value
     */
    long value();

}
