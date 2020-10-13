package com.github.fmjsjx.libnetty.resp3;

import com.github.fmjsjx.libnetty.resp.RespMessage;

/**
 * An interface that defines an RESP3 message, providing common properties and
 * methods for RESP3 messages.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3Message extends RespMessage, Resp3Object {

    /**
     * Returns the type of this {@link Resp3Message}.
     * 
     * @return a {@link Resp3MessageType}
     */
    @Override
    Resp3MessageType type();

}
