package com.github.fmjsjx.libnetty.http.server.exception;

import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Abstract implementation of {@link HttpRuntimeException} with a
 * {@link HttpResponseStatus}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public abstract class HttpFailureException extends HttpRuntimeException {

    private static final long serialVersionUID = -1392803129128681473L;

    protected HttpFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    protected HttpFailureException(String message) {
        super(message);
    }

    protected HttpFailureException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    /**
     * Returns the failure status.
     * 
     * @return the failure status
     */
    public abstract HttpResponseStatus status();

    @Override
    public String getLocalizedMessage() {
        String msg = getMessage();
        return msg == null ? status().toString() : status() + ": " + getMessage();
    }

}
