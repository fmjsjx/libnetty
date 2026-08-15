package com.github.fmjsjx.libnetty.resp3;

import java.math.BigDecimal;

import com.github.fmjsjx.libnetty.resp.AbstractSimpleRespMessage;

import io.netty.util.AsciiString;

/**
 * The default implementation of {@link Resp3DoubleMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultDoubleMessage extends AbstractSimpleRespMessage implements Resp3DoubleMessage {

    private final BigDecimal value;
    private AsciiString cachedString;

    /**
     * Constructs a new {@link DefaultDoubleMessage} instance holding the specified
     * double value.
     * 
     * @param value the double value
     */
    public DefaultDoubleMessage(BigDecimal value) {
        this.value = value;
    }

    /**
     * Constructs a new {@link DefaultDoubleMessage} instance holding the double
     * value represented by the argument string {@code s}.
     * 
     * @param s the string to be parsed
     */
    public DefaultDoubleMessage(String s) {
        this.value = new BigDecimal(s);
        this.cachedString = AsciiString.cached(s);
    }

    @Override
    public boolean isInfinity() {
        return false;
    }

    @Override
    public boolean isPostivieInfinity() {
        return false;
    }

    @Override
    public boolean isNegativeInfinity() {
        return false;
    }

    @Override
    public BigDecimal value() {
        return value;
    }

    @Override
    protected byte[] encodedValue() throws Exception {
        return cachedString().array();
    }

    private AsciiString cachedString() {
        AsciiString cachedString = this.cachedString;
        if (cachedString == null) {
            this.cachedString = cachedString = AsciiString.cached(value.toString());
        }
        return cachedString;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + cachedString() + "]";
    }

}
