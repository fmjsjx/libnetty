package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public interface CachedRespMessage extends RespMessage, RespContent {

    @Override
    default void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        encode(out);
    }

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
