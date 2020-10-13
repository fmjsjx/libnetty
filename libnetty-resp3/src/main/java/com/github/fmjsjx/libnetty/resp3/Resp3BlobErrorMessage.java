package com.github.fmjsjx.libnetty.resp3;

import com.github.fmjsjx.libnetty.resp.RespContent;

/**
 * An interface defines a RESP3 Blob Error message. Combines the
 * {@link Resp3Message} and {@link RespContent}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3BlobErrorMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.BLOB_ERROR;
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
