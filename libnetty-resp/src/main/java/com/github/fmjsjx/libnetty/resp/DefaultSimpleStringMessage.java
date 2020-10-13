package com.github.fmjsjx.libnetty.resp;

import io.netty.util.CharsetUtil;

/**
 * The default implementation of {@link RespSimpleStringMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultSimpleStringMessage extends AbstractSimpleRespMessage implements RespSimpleStringMessage {

    private final String value;

    /**
     * Constructs a new {@link DefaultBulkStringMessage} instance with the specified
     * value.
     * 
     * @param value the value
     */
    public DefaultSimpleStringMessage(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    protected byte[] encodedValue() throws Exception {
        return value.getBytes(CharsetUtil.UTF_8);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
