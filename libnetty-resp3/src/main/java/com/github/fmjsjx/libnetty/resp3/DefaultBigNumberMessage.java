package com.github.fmjsjx.libnetty.resp3;

import java.math.BigInteger;

import com.github.fmjsjx.libnetty.resp.AbstractSimpleRespMessage;

import io.netty.util.CharsetUtil;

/**
 * The default implementation of {@link Resp3BigNumberMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultBigNumberMessage extends AbstractSimpleRespMessage implements Resp3BigNumberMessage {

    /**
     * Create a new {@link DefaultBigNumberMessage}.
     * <p>
     * Translates the decimal String representation of a BigInteger into a
     * BigInteger.
     * 
     * @param value the decimal String representation of BigInteger
     * @return a {@code DefaultBigNumberMessage}
     */
    public static final DefaultBigNumberMessage create(String value) {
        return new DefaultBigNumberMessage(new BigInteger(value), value);
    }

    private final BigInteger value;
    private String text;

    /**
     * Constructs a new {@link DefaultBlobErrorMessage} with the specified value.
     * 
     * @param value the big integer value
     */
    public DefaultBigNumberMessage(BigInteger value) {
        this.value = value;
    }

    /**
     * Constructs a new {@link DefaultBlobErrorMessage} with the specified value and
     * text.
     * 
     * @param value the big integer value
     * @param text  the decimal String representation of BigInteger
     */
    public DefaultBigNumberMessage(BigInteger value, String text) {
        super();
        this.value = value;
        this.text = text;
    }

    @Override
    public BigInteger value() {
        return value;
    }

    @Override
    public String textValue() {
        String text = this.text;
        if (text == null) {
            this.text = text = value.toString();
        }
        return text;
    }

    @Override
    protected byte[] encodedValue() throws Exception {
        return textValue().getBytes(CharsetUtil.US_ASCII);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + textValue() + "]";
    }

}
