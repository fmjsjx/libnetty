package com.github.fmjsjx.libnetty.http.client;

/**
 * Main interface for an HTTP client.
 * 
 * @author fmjsjx
 * 
 * @since 1.0
 */
public interface HttpClient extends AutoCloseable {

    /**
     * Close this HTTP client.
     * 
     * @since 1.0
     */
    @Override
    default void close() {
        // default do nothing
    }

    /**
     * HTTP response.
     * 
     * @author fmjsjx
     *
     * @param <T> Type of response content
     * 
     * @since 1.0
     */
    interface Response<T> {

    }

}
