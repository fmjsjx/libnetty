package com.github.fmjsjx.libnetty.resp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public interface RespBulkStringMessage extends RespMessage, RespContent {

    boolean isNull();

    int intValue();

    long longValue();

    double doubleValue();

    BigInteger bigIntegerValue();

    BigDecimal bigDecimalValue();

    String textValue(Charset charset);

    default String textValue() {
        return textValue(CharsetUtil.UTF_8);
    }

    AsciiString asciiValue();

    @Override
    RespBulkStringMessage copy();

    @Override
    RespBulkStringMessage duplicate();

    @Override
    RespBulkStringMessage retainedDuplicate();

    @Override
    RespBulkStringMessage replace(ByteBuf content);

    @Override
    RespBulkStringMessage retain();

    @Override
    RespBulkStringMessage retain(int increment);

    @Override
    RespBulkStringMessage touch();

    @Override
    RespBulkStringMessage touch(Object hint);

}
