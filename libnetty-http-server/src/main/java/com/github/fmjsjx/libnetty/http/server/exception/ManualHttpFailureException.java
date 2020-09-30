package com.github.fmjsjx.libnetty.http.server.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * An {@link HttpFailureException} with manual response contents.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class ManualHttpFailureException extends HttpFailureException {

    private static final long serialVersionUID = -7247664259899283220L;

    private final HttpResponseStatus status;
    private final String content;
    private final CharSequence contentType;

    /**
     * Constructs a new {@link ManualHttpFailureException} with the specified
     * status, content, contentType and cause.
     * 
     * @param status      the failure status
     * @param content     the response content string
     * @param contentType the response content type
     * @param cause       the cause
     */
    public ManualHttpFailureException(HttpResponseStatus status, String content, CharSequence contentType,
            Throwable cause) {
        super(cause);
        this.status = status;
        this.content = content;
        this.contentType = contentType;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public String getLocalizedMessage() {
        return status + ": " + content;
    }

    /**
     * Returns the response content string.
     * 
     * @return the response content string
     */
    public String content() {
        return content;
    }

    /**
     * Returns the response content type.
     * 
     * @return the response content type
     */
    public CharSequence contentType() {
        return contentType;
    }

}
