package com.github.fmjsjx.libnetty.resp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

/**
 * An interface defines a RESP Simple String message. Combines the
 * {@link RespMessage} and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author fmjsjx
 */
public interface RespSimpleStringMessage extends RespMessage, RespContent {

    /**
     * Returns the value string.
     * 
     * @return the value
     */
    String value();

    /**
     * Returns the {@link Charset} of the value string.
     * 
     * @return a {@code charset}
     */
    Charset charset();

    @Override
    RespSimpleStringMessage copy();

    @Override
    RespSimpleStringMessage duplicate();

    @Override
    RespSimpleStringMessage retainedDuplicate();

    @Override
    RespSimpleStringMessage replace(ByteBuf content);

    @Override
    RespSimpleStringMessage retain();

    @Override
    RespSimpleStringMessage retain(int increment);

    @Override
    RespSimpleStringMessage touch();

    @Override
    RespSimpleStringMessage touch(Object hint);

}
