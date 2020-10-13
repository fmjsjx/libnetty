package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Simple String message.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespSimpleStringMessage extends RespMessage {

    @Override
    default RespMessageType type() {
        return RespMessageType.SIMPLE_STRING;
    }

    /**
     * Returns the value string.
     * 
     * @return the value
     */
    String value();

}
