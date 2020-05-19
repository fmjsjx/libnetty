package com.github.fmjsjx.libnetty.http.client.exception;

/**
 * Base HTTP runtime exception.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public class HttpRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -1551443950470402372L;

    /**
     * Constructs a new HTTP runtime exception with the specified detail message and
     * cause.
     * 
     * @param message the detail message
     * @param cause   the cause
     */
    public HttpRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new HTTP runtime exception with the specified cause.
     * 
     * @param cause the cause
     */
    public HttpRuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new HTTP runtime exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public HttpRuntimeException(String message) {
        super(message);
    }

}
