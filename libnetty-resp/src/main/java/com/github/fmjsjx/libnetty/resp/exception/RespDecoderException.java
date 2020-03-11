package com.github.fmjsjx.libnetty.resp.exception;

import java.util.function.Supplier;

import io.netty.handler.codec.DecoderException;

public class RespDecoderException extends DecoderException implements Supplier<RespDecoderException> {

    private static final long serialVersionUID = 1L;

    public RespDecoderException() {
        super();
    }

    public RespDecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public RespDecoderException(String message) {
        super(message);
    }

    public RespDecoderException(Throwable cause) {
        super(cause);
    }

    @Override
    public RespDecoderException get() {
        return this;
    }

}
