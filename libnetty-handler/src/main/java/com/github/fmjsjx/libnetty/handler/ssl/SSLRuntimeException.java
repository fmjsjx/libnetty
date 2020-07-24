package com.github.fmjsjx.libnetty.handler.ssl;

import javax.net.ssl.SSLException;

/**
 * A runtime exception for SSL.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class SSLRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -7658659367655682220L;

    /**
     * Constructs a new SSL runtime exception with the specified detail message and
     * cause (as {@link SSLException}).
     * 
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method).
     * @param cause   the cause as {@link SSLException}
     */
    public SSLRuntimeException(String message, SSLException cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SSL runtime exception with the specified cause (as
     * {@link SSLException}).
     * 
     * @param cause the cause as {@link SSLException}
     */
    public SSLRuntimeException(SSLException cause) {
        super(cause);
    }

    @Override
    public SSLException getCause() {
        return (SSLException) super.getCause();
    }

}
