package com.github.fmjsjx.libnetty.http.server;

import java.time.ZonedDateTime;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Default implementation of {@link HttpResult}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultHttpResult implements HttpResult {

    private final HttpRequestContext requestContext;
    private final long resultLength;
    private final HttpResponseStatus responseStatus;
    private final long respondedNaonTime;
    private final ZonedDateTime respondedTime;

    /**
     * Creates a new {@link DefaultHttpResult} instance with the specified params
     * given.
     * 
     * @param requestContext an {@link HttpRequestContext}
     * @param resultLength   the length of the HTTP response body content
     * @param responseStatus the {@link HttpResponseStatus} of the HTTP response
     */
    public DefaultHttpResult(HttpRequestContext requestContext, long resultLength, HttpResponseStatus responseStatus) {
        this(requestContext, resultLength, responseStatus, System.nanoTime(),
                ZonedDateTime.now(requestContext.receivedTime().getZone()));
    }

    /**
     * Creates a new {@link DefaultHttpResult} instance with the specified params
     * given.
     * 
     * @param requestContext    an {@link HttpRequestContext}
     * @param resultLength      the length of the HTTP response body content
     * @param responseStatus    the {@link HttpResponseStatus} of the HTTP response
     * @param respondedNaonTime the value of the running Java Virtual
     *                          Machine'shigh-resolution time source, in
     *                          nanoseconds, when the HTTP response just responded
     * @param respondedTime     the {@link ZonedDateTime} when the HTTP response
     *                          just responded
     */
    public DefaultHttpResult(HttpRequestContext requestContext, long resultLength, HttpResponseStatus responseStatus,
            long respondedNaonTime, ZonedDateTime respondedTime) {
        this.requestContext = requestContext;
        this.resultLength = resultLength;
        this.responseStatus = responseStatus;
        this.respondedNaonTime = respondedNaonTime;
        this.respondedTime = respondedTime;
    }

    @Override
    public HttpRequestContext requestContext() {
        return requestContext;
    }

    @Override
    public long resultLength() {
        return resultLength;
    }

    @Override
    public HttpResponseStatus responseStatus() {
        return responseStatus;
    }

    @Override
    public long respondedNaonTime() {
        return respondedNaonTime;
    }

    @Override
    public ZonedDateTime respondedTime() {
        return respondedTime;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder().append("DefaultHttpResult(resultLength: ").append(resultLength)
                .append(", responseStatus: ").append(responseStatus).append(", respondedNaonTime: ")
                .append(respondedNaonTime).append(", respondedTime: ").append(respondedTime).append(")");
        return b.toString();
    }

}
