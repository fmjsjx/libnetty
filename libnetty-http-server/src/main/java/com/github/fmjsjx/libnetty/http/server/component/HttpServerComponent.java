package com.github.fmjsjx.libnetty.http.server.component;

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

}
