package com.github.fmjsjx.libnetty.http.server.component;

import com.github.fmjsjx.libnetty.http.server.HttpServer;

/**
 * Defines an interface of HTTP server components.
 * 
 * @since 1.3
 *
 * @author MJ Fang
 */
public interface HttpServerComponent {

    /**
     * Returns the type of the component.
     * 
     * @return the type of the component
     */
    Class<? extends HttpServerComponent> componentType();

    /**
     * This method will be invoked by framework when the {@link HttpServer} which
     * this {@link HttpServerComponent} belongs to is just be closed.
     * <p>
     * This method is equivalent to {@link #close}.
     * 
     * @throws Exception if any error occurs
     */
    default void onServerClosed() throws Exception {
        close();
    }

    /**
     * Closes this {@link HttpServerComponent} and releases any system resources
     * associated with it.
     * <p>
     * Default do nothing.
     * 
     * @throws Exception if any error occurs
     */
    default void close() throws Exception {
        // default do nothing
    }

}
