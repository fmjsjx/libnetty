package com.github.fmjsjx.libnetty.http.server;

import java.time.ZonedDateTime;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Default implementation of {@link HttpResult}.
 *
 * @author MJ Fang
 * @since 1.1
 */
public class DefaultHttpResult implements HttpResult {

    private final HttpRequestContext requestContext;
    private final long resultLength;
    private final HttpResponseStatus responseStatus;
    private final long respondedNanoTime;
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
     * @param respondedNanoTime the value of the running Java Virtual
     *                          Machine's high-resolution time source, in
     *                          nanoseconds, when the HTTP response just responded
     * @param respondedTime     the {@link ZonedDateTime} when the HTTP response
     *                          just responded
     */
    public DefaultHttpResult(HttpRequestContext requestContext, long resultLength, HttpResponseStatus responseStatus,
                             long respondedNanoTime, ZonedDateTime respondedTime) {
        this.requestContext = requestContext;
        this.resultLength = resultLength;
        this.responseStatus = responseStatus;
        this.respondedNanoTime = respondedNanoTime;
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
    public long respondedNanoTime() {
        return respondedNanoTime;
    }

    @Override
    public ZonedDateTime respondedTime() {
        return respondedTime;
    }

    @Override
    public String toString() {
        return "DefaultHttpResult(resultLength: " + resultLength +
                ", responseStatus: " + responseStatus +
                ", respondedNanoTime: " + respondedNanoTime +
                ", respondedTime: " + respondedTime + ")";
    }

}
