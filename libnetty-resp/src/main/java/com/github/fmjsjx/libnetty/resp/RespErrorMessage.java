package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Error message.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespErrorMessage extends RespMessage {

    @Override
    default RespMessageType type() {
        return RespMessageType.ERROR;
    }

    /**
     * Returns the error code string.
     * 
     * @return the error code
     */
    CharSequence code();

    /**
     * Returns the error message.
     * 
     * @return the error message
     */
    String message();

    /**
     * Returns the full text string of this error.
     * 
     * @return the full text string of this error
     */
    String text();

}
