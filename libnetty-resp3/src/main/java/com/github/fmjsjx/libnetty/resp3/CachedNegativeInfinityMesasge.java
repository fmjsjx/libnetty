package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.NEGATIVE_INFINITY;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

/**
 * The cached implementation of {@link Resp3DoubleMessage} for negative
 * infinity.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedNegativeInfinityMesasge extends CachedRespMessage implements Resp3DoubleMessage {

    static final CachedNegativeInfinityMesasge INSTANCE = new CachedNegativeInfinityMesasge();

    /**
     * Returns the singleton {@link CachedNegativeInfinityMesasge} instance.
     * 
     * @return the singleton {@link CachedNegativeInfinityMesasge} instance
     */
    public static final CachedNegativeInfinityMesasge getInstance() {
        return INSTANCE;
    }

    private CachedNegativeInfinityMesasge() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + NEGATIVE_INFINITY.length() + EOL_LENGTH)
                .writeByte(Resp3MessageType.DOUBLE.value()).writeBytes(NEGATIVE_INFINITY.array())
                .writeShort(EOL_SHORT));
    }

    @Override
    public boolean isInfinity() {
        return true;
    }

    @Override
    public double value() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value() + "]";
    }

}
