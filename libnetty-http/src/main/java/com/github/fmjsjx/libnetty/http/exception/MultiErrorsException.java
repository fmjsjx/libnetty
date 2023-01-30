package com.github.fmjsjx.libnetty.http.exception;

import java.io.Serial;

/**
 * An exception holding multiply errors.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class MultiErrorsException extends HttpRuntimeException {

    @Serial
    private static final long serialVersionUID = -920326062016826505L;

    /**
     * The errors.
     */
    private final Throwable[] errors;

    /**
     * Constructs a new {@link MultiErrorsException} with the specified detail message and
     * errors.
     *
     * @param message the detail message
     * @param errors   the errors
     */
    public MultiErrorsException(String message, Throwable... errors) {
        super(message);
        this.errors = errors;
    }

    /**
     * Returns errors.
     *
     * @return errors
     */
    public Throwable[] errors() {
        return errors;
    }

}
