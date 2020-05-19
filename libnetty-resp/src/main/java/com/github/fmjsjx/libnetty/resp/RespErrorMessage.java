package com.github.fmjsjx.libnetty.resp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;

/**
 * An interface defines a RESP Error message. Combines the {@link RespMessage}
 * and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespErrorMessage extends RespMessage, RespContent {

    /**
     * Returns the error code string as {@link AsciiString} type.
     * 
     * @return the error code
     */
    AsciiString code();

    /**
     * Returns the error message.
     * 
     * @return the error message
     */
    String message();

    /**
     * Returns the {@link Charset} of the error message.
     * 
     * @return a {@code charset}
     */
    Charset charset();

    /**
     * Returns the full text string of this error.
     * 
     * @return the full text string of this error
     */
    String text();

    @Override
    RespErrorMessage copy();

    @Override
    RespErrorMessage duplicate();

    @Override
    RespErrorMessage retainedDuplicate();

    @Override
    RespErrorMessage replace(ByteBuf content);

    @Override
    RespErrorMessage retain();

    @Override
    RespErrorMessage retain(int increment);

    @Override
    RespErrorMessage touch();

    @Override
    RespErrorMessage touch(Object hint);

}
