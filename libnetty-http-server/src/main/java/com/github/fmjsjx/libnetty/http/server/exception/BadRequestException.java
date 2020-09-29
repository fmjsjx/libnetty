package com.github.fmjsjx.libnetty.http.server.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * HTTP exception for 404 Bad Request.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class BadRequestException extends HttpFailureException {

    private static final long serialVersionUID = -5451724237434938174L;

    /**
     * Constructs a new HTTP bad request exception with the specified cause.
     * 
     * @param cause the cause
     */
    public BadRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public HttpResponseStatus status() {
        return HttpResponseStatus.BAD_REQUEST;
    }

}
