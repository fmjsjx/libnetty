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

    private final double value;
    private final AsciiString cachedString;
    private BigDecimal cachedBigDecimal;

    /**
     * Constructs a new {@link DefaultDoubleMessage} instance holding the specified
     * double value.
     * 
     * @param value the double value
     */
    public DefaultDoubleMessage(double value) {
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException("value can't be infinity");
        }
        this.value = value;
        this.cachedString = AsciiString.cached(Double.toString(value));
    }

    /**
     * Constructs a new {@link DefaultDoubleMessage} instance holding the double
     * value represented by the argument string {@code s}.
     * 
     * @param s the string to be parsed
     */
    public DefaultDoubleMessage(String s) {
        this.value = Double.parseDouble(s);
        this.cachedString = AsciiString.cached(s);
    }

    @Override
    public boolean isInfinity() {
        return false;
    }

    @Override
    public double value() {
        return value;
    }

    @Override
    protected byte[] encodedValue() throws Exception {
        return cachedString.array();
    }
    
    @Override
    public BigDecimal bigDecimalValue() {
        BigDecimal decimal = this.cachedBigDecimal;
        if (decimal == null) {
            this.cachedBigDecimal = decimal = new BigDecimal(cachedString.toString());
        }
        return decimal;
        
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + cachedString + "]";
    }

}
