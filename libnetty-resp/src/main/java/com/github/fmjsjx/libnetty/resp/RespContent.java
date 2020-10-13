package com.github.fmjsjx.libnetty.resp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
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
     * Exception: {@code "Can't convert to ASCII string."}
     */
    static IllegalArgumentException CANT_CONVERT_TO_ASCII = new IllegalArgumentException(
            "Can't convert to ASCII string.");

    /**
     * Returns the content value as {@link Integer} type.
     * 
     * @return an {@code Integer} value
     */
    default Integer toInteger() {
        return RespCodecUtil.decodeInt(content());
    }

    /**
     * Returns the content value as {@link Long} type.
     * 
     * @return a {@code Long} value
     */
    default Long toLong() {
        return RespCodecUtil.decodeLong(content());
    }

    /**
     * Returns the content value as {@link Double} type.
     * 
     * @return a {@code Double} value
     */
    default Double toDouble() {
        return Double.valueOf(toText(CharsetUtil.US_ASCII));
    }

    /**
     * Returns the content value as {@link BigInteger} type.
     * 
     * @return a {@code BigInteger} value
     */
    default BigInteger toBigInteger() {
        return new BigInteger(toText(CharsetUtil.US_ASCII));
    }

    /**
     * Returns the content value as {@link BigDecimal} type.
     * 
     * @return a {@code BigDecimal} value
     */
    default BigDecimal toBigDecimal() {
        return new BigDecimal(toText(CharsetUtil.US_ASCII));
    }

    /**
     * Returns the content value as {@link String} type decoded with given
     * {@code charset}.
     * 
     * @param charset a {@link Charset}
     * @return a {@code String} value
     */
    default String toText(Charset charset) {
        return content().toString(charset);
    }

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
    default AsciiString toAscii() {
        ByteBuf data = content();
        if (ByteBufUtil.isText(data, CharsetUtil.US_ASCII)) {
            return AsciiString.cached(toText(CharsetUtil.UTF_8));
        }
        throw CANT_CONVERT_TO_ASCII;
    }

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
