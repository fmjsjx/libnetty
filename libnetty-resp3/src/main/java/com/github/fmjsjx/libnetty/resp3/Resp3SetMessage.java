package com.github.fmjsjx.libnetty.resp3;

import com.github.fmjsjx.libnetty.resp.RespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;

/**
 * An interface defines a RESP3 Set message.
 * 
 * @param <E> the type of values in the message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3SetMessage<E extends RespMessage> extends RespAggregateMessage<E> {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.SET;
    }

}
