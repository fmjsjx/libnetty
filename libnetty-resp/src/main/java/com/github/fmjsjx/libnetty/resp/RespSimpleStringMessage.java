package com.github.fmjsjx.libnetty.resp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

public interface RespSimpleStringMessage extends RespMessage, RespContent {

    String value();
    
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
