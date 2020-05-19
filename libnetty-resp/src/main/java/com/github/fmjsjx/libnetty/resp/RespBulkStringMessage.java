package com.github.fmjsjx.libnetty.resp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * An interface defines a RESP Bulk String message. Combines the
 * {@link RespMessage} and {@link RespContent}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespBulkStringMessage extends RespMessage, RespContent {

    /**
     * Returns {@code true} if and only if this is {@code nil}.
     * 
     * @return {@code true} if {@code nil}
     */
    boolean isNull();

    /**
     * Returns this value as {@code int} type.
     * 
     * @return a {@code int} value
     */
    int intValue();

    /**
     * Returns this value as {@code long} type.
     * 
     * @return a {@code long} value
     */
    long longValue();

    /**
     * Returns this value as {@code double} type.
     * 
     * @return a {@code double} value
     */
    double doubleValue();

    /**
     * Returns this value as {@link BigInteger} type.
     * 
     * @return a {@code BigInteger} value
     */
    BigInteger bigIntegerValue();

    /**
     * Returns this value as {@link BigDecimal} type.
     * 
     * @return a {@code BigDecimal} value
     */
    BigDecimal bigDecimalValue();

    /**
     * Returns this value as {@link String} type decoded with the specific
     * {@link Charset} given.
     * 
     * @param charset a {@code Charset}
     * @return a {@code String} value
     */
    String textValue(Charset charset);

    /**
     * Returns this value as {@link String} type decoded with {@code UTF-8}
     * character set.
     * 
     * @return a {@code String} value
     */
    default String textValue() {
        return textValue(CharsetUtil.UTF_8);
    }

    /**
     * Returns this value as {@link AsciiString} type.
     * 
     * @return a {@code AsciiString} value
     */
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
