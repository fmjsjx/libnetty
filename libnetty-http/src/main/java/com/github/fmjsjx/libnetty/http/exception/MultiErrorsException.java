package com.github.fmjsjx.libnetty.http.exception;

/**
 * An exception holding multiply errors.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class MultiErrorsException extends HttpRuntimeException {

    private static final long serialVersionUID = -920326062016826505L;

    private final Throwable[] errors;

    public MultiErrorsException(String message, Throwable... errors) {
        super(message);
        this.errors = errors;
    }

    public Throwable[] errors() {
        return errors;
    }

}
