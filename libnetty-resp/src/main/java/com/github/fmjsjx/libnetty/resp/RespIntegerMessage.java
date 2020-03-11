package com.github.fmjsjx.libnetty.resp;

import io.netty.buffer.ByteBuf;

public interface RespIntegerMessage extends RespMessage, RespContent {

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
