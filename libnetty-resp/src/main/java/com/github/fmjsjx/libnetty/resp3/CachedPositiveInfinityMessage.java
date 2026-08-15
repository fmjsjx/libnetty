package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.POSITIVE_INFINITY;

import java.math.BigDecimal;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * The cached implementation of {@link Resp3DoubleMessage} for positive
 * infinity.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedPositiveInfinityMessage extends CachedRespMessage implements Resp3DoubleMessage {

    static final CachedPositiveInfinityMessage INSTANCE = new CachedPositiveInfinityMessage();

    /**
     * Returns the singleton {@link CachedPositiveInfinityMessage} instance.
     * 
     * @return the singleton {@link CachedPositiveInfinityMessage} instance
     */
    public static final CachedPositiveInfinityMessage getInstance() {
        return INSTANCE;
    }

    private CachedPositiveInfinityMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + POSITIVE_INFINITY.length() + EOL_LENGTH)
                .writeByte(Resp3MessageType.DOUBLE.value()).writeBytes(POSITIVE_INFINITY.array())
                .writeShort(EOL_SHORT));
    }

    @Override
    public boolean isInfinity() {
        return true;
    }

    @Override
    public boolean isNegativeInfinity() {
        return false;
    }

    @Override
    public boolean isPostivieInfinity() {
        return true;
    }

    @Override
    public BigDecimal value() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value() + "]";
    }

}
