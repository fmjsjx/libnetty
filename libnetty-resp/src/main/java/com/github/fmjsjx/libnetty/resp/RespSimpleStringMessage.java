package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Simple String message. Combines the
 * {@link RespMessage} and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespSimpleStringMessage extends RespMessage {

    /**
     * Returns the value string.
     * 
     * @return the value
     */
    String value();

}
