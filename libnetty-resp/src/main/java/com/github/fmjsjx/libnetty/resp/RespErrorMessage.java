package com.github.fmjsjx.libnetty.resp;

/**
 * An interface defines a RESP Error message. Combines the {@link RespMessage}
 * and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespErrorMessage extends RespMessage {

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
