package com.github.fmjsjx.libnetty.http.client.exception;

import com.github.fmjsjx.libnetty.http.client.HttpClient;

/**
 * This exception is thrown when invoke a closed {@link HttpClient}.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public class ClientClosedException extends HttpRuntimeException {

    private static final long serialVersionUID = 2583991732362352884L;

    /**
     * Constructs a new exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public ClientClosedException(String message) {
        super(message);
    }

}
