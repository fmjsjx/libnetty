package com.github.fmjsjx.libnetty.resp.exception;

import java.util.function.Supplier;

import io.netty.handler.codec.DecoderException;

/**
 * A {@link RespDecoderException} thrown by a RESP decoder.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public class RespDecoderException extends DecoderException implements Supplier<RespDecoderException> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     */
    public RespDecoderException() {
        super();
    }

    /**
     * Creates a new instance.
     * 
     * @param message the detail message
     * @param cause   the cause
     */
    public RespDecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance.
     * 
     * @param message the detail message
     */
    public RespDecoderException(String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     * 
     * @param cause the cause
     */
    public RespDecoderException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns this.
     */
    @Override
    public RespDecoderException get() {
        return this;
    }

}
