package com.github.fmjsjx.libnetty.http.server;

import io.netty.handler.codec.http.HttpMethod;

/**
 * Wrappers for {@link HttpMethod}s.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public enum HttpMethodWrapper {
    /**
     * The OPTIONS method.
     */
    OPTIONS(HttpMethod.OPTIONS),
    /**
     * The GET method.
     */
    GET(HttpMethod.GET),
    /**
     * The HEAD method.
     */
    HEAD(HttpMethod.HEAD),
    /**
     * The POST method.
     */
    POST(HttpMethod.POST),
    /**
     * The PUT method.
     */
    PUT(HttpMethod.PUT),
    /**
     * The PATCH method.
     */
    PATCH(HttpMethod.PATCH),
    /**
     * The DELETE method.
     */
    DELETE(HttpMethod.DELETE),
    /**
     * The TRACE method.
     */
    TRACE(HttpMethod.TRACE),
    /**
     * The CONNECT method.
     */
    CONNECT(HttpMethod.CONNECT);

    private final HttpMethod wrapped;

    private HttpMethodWrapper(HttpMethod wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Returns the wrapped {@link HttpMethod}.
     * 
     * @return a {@code HttpMethod}
     */
    public HttpMethod wrapped() {
        return wrapped;
    }

}
