package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * An interface defines a cached RESP message. Combine the {@link RespMessage}
 * and {@link RespContent}.
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
public interface CachedRespMessage extends RespMessage, RespContent {

    @Override
    default void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        encode(out);
    }

    /**
     * Encode this message.
     * 
     * @param out the {@link List} into which the encoded msg should be added
     * @throws Exception is thrown if an error occurs
     */
    void encode(List<Object> out) throws Exception;

    @Override
    CachedRespMessage copy();

    @Override
    CachedRespMessage duplicate();

    @Override
    CachedRespMessage retainedDuplicate();

    @Override
    CachedRespMessage replace(ByteBuf content);

    @Override
    CachedRespMessage retain();

    @Override
    CachedRespMessage retain(int increment);

    @Override
    CachedRespMessage touch();

    @Override
    CachedRespMessage touch(Object hint);

}
