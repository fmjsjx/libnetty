package com.github.fmjsjx.libnetty.resp3;

import com.github.fmjsjx.libnetty.resp.RespAggregateMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;

/**
 * An interface defines a RESP3 Map message.
 * 
 * @param <F> the type of fields in this map message
 * @param <V> the type of values in this map message
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3MapMessage<F extends RespMessage, V extends RespMessage>
        extends RespAggregateMessage<FieldValuePair<F, V>> {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.MAP;
    }

}
