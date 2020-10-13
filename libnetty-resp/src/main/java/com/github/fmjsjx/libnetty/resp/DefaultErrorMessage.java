package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * The default implementation of {@link RespErrorMessage}.
 *
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultErrorMessage extends AbstractSimpleRespMessage implements RespErrorMessage {

    /**
     * Creates a new {@link DefaultErrorMessage} with the {@code ERR} code and the
     * {@code message}.
     * 
     * @param message the error message
     * @return a {@code DefaultErrorMessage}
     */
    public static final DefaultErrorMessage createErr(CharSequence message) {
        return create(ERR, message);
    }

    /**
     * Creates a new {@link DefaultErrorMessage} with the specified {@code code} and
     * the {@code message}.
     * 
     * @param code    the error code
     * @param message the error message
     * @return a {@code DefaultErrorMessage}
     */
    public static final DefaultErrorMessage create(CharSequence code, CharSequence message) {
        if (code instanceof AsciiString) {
            return new DefaultErrorMessage(((AsciiString) code).toUpperCase(), message.toString());
        } else {
            AsciiString acode = AsciiString.cached(code.toString().toUpperCase());
            return new DefaultErrorMessage(acode, message.toString());
        }
    }

    private final CharSequence code;
    private final String message;
    private final String text;

    /**
     * Constructs a new {@link DefaultErrorMessage} instance with the specified code
     * and message.
     * 
     * @param code    the error code
     * @param message the error message
     */
    public DefaultErrorMessage(CharSequence code, String message) {
        this(code, message, code + " " + message);
    }

    /**
     * Constructs a new {@link DefaultErrorMessage} instance with the specified code
     * , message and text.
     * 
     * @param code    the error code
     * @param message the error message
     * @param text    the full text string
     */
    public DefaultErrorMessage(CharSequence code, String message, String text) {
        this.code = code;
        this.message = message;
        this.text = text;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.ERROR;
    }

    @Override
    public CharSequence code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    protected byte[] encodedValue() throws Exception {
        return text.getBytes(CharsetUtil.UTF_8);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + text() + "]";
    }

}
