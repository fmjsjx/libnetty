package com.github.fmjsjx.libnetty.resp3;

import java.math.BigInteger;

/**
 * An interface defines a RESP3 Big Number message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3BigNumberMessage extends Resp3Message {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.BIG_NUMBER;
    }

    /**
     * Returns the big integer value.
     * 
     * @return the big integer value
     */
    BigInteger value();

    /**
     * Returns the text value of this big number.
     * 
     * @return the text value of thie big number
     */
    String textValue();

}
