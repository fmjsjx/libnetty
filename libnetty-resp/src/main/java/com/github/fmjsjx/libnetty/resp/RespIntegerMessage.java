package com.github.fmjsjx.libnetty.resp;

import io.netty.buffer.ByteBuf;

/**
 * An interface defines a RESP Integer message. Combines the {@link RespMessage}
 * and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespIntegerMessage extends RespMessage, RespContent {

    /**
     * Returns the value.
     * 
     * @return the value
     */
    long value();

    @Override
    RespIntegerMessage copy();

    @Override
    RespIntegerMessage duplicate();

    @Override
    RespIntegerMessage retainedDuplicate();

    @Override
    RespIntegerMessage replace(ByteBuf content);

    @Override
    RespIntegerMessage retain();

    @Override
    RespIntegerMessage retain(int increment);

    @Override
    RespIntegerMessage touch();

    @Override
    RespIntegerMessage touch(Object hint);

}
