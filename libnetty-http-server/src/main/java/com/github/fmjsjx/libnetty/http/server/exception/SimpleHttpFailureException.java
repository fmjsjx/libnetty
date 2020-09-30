package com.github.fmjsjx.libnetty.http.server.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * The default implementation of {@link HttpFailureException}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class SimpleHttpFailureException extends HttpFailureException {

    private static final long serialVersionUID = 5524273483668587550L;

    private final HttpResponseStatus status;

    /**
     * Constructs a new {@link SimpleHttpFailureException} instance with the
     * specified status, message and cause.
     * 
     * @param status  the failure status
     * @param message the detail message
     * @param cause   the cause
     */
    public SimpleHttpFailureException(HttpResponseStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Constructs a new {@link SimpleHttpFailureException} instance with the
     * specified status and message.
     * 
     * @param status  the failure status
     * @param message the detail message
     */
    public SimpleHttpFailureException(HttpResponseStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Constructs a new {@link SimpleHttpFailureException} instance with the status
     * "500 Internal Server Error" and the specified message and cause.
     * 
     * @param message the detail message
     * @param cause   the cause
     */
    public SimpleHttpFailureException(String message, Throwable cause) {
        this(HttpResponseStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    /**
     * Constructs a new {@link SimpleHttpFailureException} instance with the status
     * "500 Internal Server Error" and the specified message.
     * 
     * @param message the detail message
     */
    public SimpleHttpFailureException(String message) {
        this(HttpResponseStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Constructs a new {@link SimpleHttpFailureException} instance with the status
     * "500 Internal Server Error" and the specified cause.
     * 
     * @param cause the cause
     */
    public SimpleHttpFailureException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

}
