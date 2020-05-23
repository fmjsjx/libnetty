package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.handler.codec.DecoderException;

/**
 * An {@link DecoderException} which is thrown by a FastCGI decoder.
 */
public class FcgiDecoderException extends DecoderException {

    private static final long serialVersionUID = 6851755647676850945L;

    /**
     * Creates a new instance.
     */
    public FcgiDecoderException() {
        super();
    }

    /**
     * Creates a new instance.
     */
    public FcgiDecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance.
     */
    public FcgiDecoderException(String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     */
    public FcgiDecoderException(Throwable cause) {
        super(cause);
    }

}
