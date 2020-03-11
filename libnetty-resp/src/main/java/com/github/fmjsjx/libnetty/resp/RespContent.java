package com.github.fmjsjx.libnetty.resp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public interface RespContent extends RespObject, ByteBufHolder {

    Long toLong();

    Double toDouble();

    BigInteger toBigInteger();

    BigDecimal toBigDecimal();

    String toText(Charset charset);

    default String toText() {
        return toText(CharsetUtil.UTF_8);
    }
    
    AsciiString toAscii();

    @Override
    RespContent copy();

    @Override
    RespContent duplicate();

    @Override
    RespContent retainedDuplicate();

    @Override
    RespContent replace(ByteBuf content);

    @Override
    RespContent retain();

    @Override
    RespContent retain(int increment);

    @Override
    RespContent touch();

    @Override
    RespContent touch(Object hint);

}
