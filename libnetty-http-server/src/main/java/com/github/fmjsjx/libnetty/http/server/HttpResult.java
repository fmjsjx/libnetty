package com.github.fmjsjx.libnetty.http.server;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * An operation result of HTTP.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface HttpResult {

    /**
     * Returns the context of the HTTP request.
     * 
     * @return the context of the HTTP request
     */
    HttpRequestContext requestContext();

    /**
     * Returns the length of the HTTP response content.
     * 
     * @return the length of the HTTP response content
     */
    long resultLength();

    /**
     * Returns the status of the HTTP response.
     * 
     * @return the status of the HTTP response
     */
    HttpResponseStatus responseStatus();

    /**
     * Returns the value of the running Java Virtual Machine'shigh-resolution time
     * source, in nanoseconds, when the HTTP response just responded.
     * 
     * @return the value of the running Java Virtual Machine'shigh-resolution time
     *         source, in nanoseconds
     * @see System#nanoTime()
     */
    long respondedNanoTime();

    /**
     * Returns the {@link ZonedDateTime} with the system {@link ZoneId} when the
     * HTTP response just responded.
     * 
     * @return a {@code ZonedDateTime}
     */
    ZonedDateTime respondedTime();

    /**
     * Returns the {@link ZonedDateTime} with the specified {@link ZoneId} when the
     * HTTP response just responded.
     * 
     * @param zone the {@code ZoneId}
     * @return a {@code ZonedDateTime} with the specified {@code zone}
     */
    default ZonedDateTime respondedTime(ZoneId zone) {
        return respondedTime().withZoneSameLocal(zone);
    }

    /**
     * Returns the time between the request coming and when the response has
     * finished being written out to the connection, in nanoseconds.
     * 
     * @return the time between the request coming and when the response has
     *         finished being written out to the connection, in nanoseconds
     */
    default long nanoUsed() {
        return respondedNanoTime() - requestContext().receivedNanoTime();
    }

    /**
     * Returns the time between the request coming and when the response has
     * finished being written out to the connection, in specified time unit.
     * 
     * @param unit the time unit
     * @return the time between the request coming and when the response has
     *         finished being written out to the connection, in specified
     *         {@code unit}
     */
    default long timeUsed(TimeUnit unit) {
        return unit.convert(nanoUsed(), TimeUnit.NANOSECONDS);
    }

}
