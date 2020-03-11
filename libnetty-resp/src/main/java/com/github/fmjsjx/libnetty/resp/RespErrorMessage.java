package com.github.fmjsjx.libnetty.resp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;

public interface RespErrorMessage extends RespMessage, RespContent {

    AsciiString code();

    String message();

    Charset charset();

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
