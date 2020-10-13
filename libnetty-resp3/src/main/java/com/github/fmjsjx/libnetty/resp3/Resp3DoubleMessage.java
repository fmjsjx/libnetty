package com.github.fmjsjx.libnetty.resp3;

import java.math.BigDecimal;

/**
 * An interface defines a RESP3 Double message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3DoubleMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.DOUBLE;
    }

    /**
     * Returns {@code true} if this double message is positive or negative infinity
     * , {@code false} otherwise.
     * 
     * @return {@code true} if this double message is positive or negative infinity
     *         ; {@code false} otherwise.
     */
    boolean isInfinity();

    /**
     * Returns the double value.
     * 
     * @return the double value
     */
    double value();

    /**
     * Returns the value as {@link BigDecimal} type.
     * 
     * @return the value as {@link BigDecimal} type
     */
    default BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(value());
    }

}
