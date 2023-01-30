package com.github.fmjsjx.libnetty.http.server.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.Serial;

/**
 * An {@link HttpFailureException} with manual response contents.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class ManualHttpFailureException extends HttpFailureException {

    @Serial
    private static final long serialVersionUID = -7247664259899283220L;

    /**
     * The response status.
     */
    private final HttpResponseStatus status;
    /**
     * The response content string.
     */
    private final String content;
    /**
     * The response content type.
     */
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

    /**
     * Constructs a new {@link ManualHttpFailureException} with the specified
     * status, content, contentType and message.
     * 
     * @param status      the failure status
     * @param content     the response content string
     * @param contentType the response content type
     * @param message     the detail message
     */
    public ManualHttpFailureException(HttpResponseStatus status, String content, CharSequence contentType,
            String message) {
        super(message);
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
        String msg = getMessage();
        return msg == null ? status + ": " + content : status + " - " + msg + ": " + content;
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
