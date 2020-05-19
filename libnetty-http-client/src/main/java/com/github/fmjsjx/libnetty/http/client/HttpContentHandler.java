package com.github.fmjsjx.libnetty.http.client;

import java.util.function.Function;

import io.netty.buffer.ByteBuf;

/**
 * A handler for HTTP content conversions.
 *
 * @param <T> type of the content
 * 
 * @author MJ Fang
 * 
 * @since 1.0
 */
@FunctionalInterface 
public interface HttpContentHandler<T> extends Function<ByteBuf, T> {

    /**
     * Convert and returns the content value from {@link ByteBuf}.
     * 
     * @param content the HTTP content as {@link ByteBuf} type
     * @return the converted HTTP content instance
     */
    @Override
    T apply(ByteBuf content);

}