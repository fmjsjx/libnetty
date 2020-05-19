package com.github.fmjsjx.libnetty.resp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * An interface that defines content holder of RESP objects, providing common
 * methods for content value conversions.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface RespContent extends RespObject, ByteBufHolder {

    /**
     * Returns the content value as {@link Integer} type.
     * 
     * @return an {@code Integer} value
     */
    Integer toInteger();

    /**
     * Returns the content value as {@link Long} type.
     * 
     * @return a {@code Long} value
     */
    Long toLong();

    /**
     * Returns the content value as {@link Double} type.
     * 
     * @return a {@code Double} value
     */
    Double toDouble();

    /**
     * Returns the content value as {@link BigInteger} type.
     * 
     * @return a {@code BigInteger} value
     */
    BigInteger toBigInteger();

    /**
     * Returns the content value as {@link BigDecimal} type.
     * 
     * @return a {@code BigDecimal} value
     */
    BigDecimal toBigDecimal();

    /**
     * Returns the content value as {@link String} type decoded with given
     * {@code charset}.
     * 
     * @param charset a {@link Charset}
     * @return a {@code String} value
     */
    String toText(Charset charset);

    /**
     * Returns the content value as {@link String} type decoded with {@code UTF-8}
     * character set.
     * 
     * @return a {@code String} value
     */
    default String toText() {
        return toText(CharsetUtil.UTF_8);
    }

    /**
     * Returns the content value as {@link AsciiString} type.
     * 
     * @return a {@code AsciiString} value
     */
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
