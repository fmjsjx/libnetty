package com.github.fmjsjx.libnetty.resp;

/**
 * The default implementation of {@link RespIntegerMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultIntegerMessage extends AbstractSimpleRespMessage implements RespIntegerMessage {

    private final long value;

    /**
     * Constructs a new {@link DefaultBulkStringMessage} with the specified value.
     * 
     * @param value the long value
     * 
     * @since 1.1
     */
    public DefaultIntegerMessage(long value) {
        this.value = value;
    }

    @Override
    public long value() {
        return value;
    }

    @Override
    protected byte[] encodedValue() throws Exception {
        return RespCodecUtil.longToAsciiBytes(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
