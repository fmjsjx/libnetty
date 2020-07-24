package com.github.fmjsjx.libnetty.http.server;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCounted;

/**
 * A context that runs through each HTTP requests.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface HttpRequestContext extends ReferenceCounted {

    /**
     * Returns the value of the running Java Virtual Machine'shigh-resolution time
     * source, in nanoseconds, when the HTTP request just received.
     * 
     * @return the value of the running Java Virtual Machine'shigh-resolution time
     *         source, in nanoseconds
     * @see System#nanoTime()
     */
    long recievedNanoTime();

    /**
     * Returns the {@link ZonedDateTime} with the system {@link ZoneId} when the
     * HTTP request just received.
     * 
     * @return a {@code ZonedDateTime}
     */
    ZonedDateTime receivedTime();

    /**
     * Returns the {@link ZonedDateTime} with the specified {@link ZoneId} when the
     * HTTP request just received.
     * 
     * @param zone the {@code ZoneId}
     * @return a {@code ZonedDateTime} with the specified {@code zone}
     */
    default ZonedDateTime receivedTime(ZoneId zone) {
        return receivedTime().withZoneSameLocal(zone);
    }

    /**
     * Returns the {@link LocalDateTime} when the HTTP request just received.
     * 
     * @return a {@code LocalDateTime}
     */
    default LocalDateTime receivedLocalTime() {
        return receivedTime().toLocalDateTime();
    }

    /**
     * Returns the {@link Channel} which is bound to the {@link HttpRequestContext}.
     * 
     * @return a {@code Channel}
     */
    Channel channel();

    /**
     * Return the {@link EventLoop} this {@link Channel} was registered to.
     * 
     * @return a {@code EventLoop}
     */
    default EventLoop eventLoop() {
        return channel().eventLoop();
    }

    /**
     * Returns the remote address (client IP) of this {@link HttpRequestContext}.
     * <p>
     * All implementations should fix with the HTTP PROXY header
     * {@code "x-forwarded-for"}.
     * 
     * @return the remote address
     */
    String remoteAddress();

    /**
     * Returns the {@link FullHttpRequest} which is bound to the
     * {@link HttpRequestContext}.
     * 
     * @return a {@code FullHttpRequest}
     */
    FullHttpRequest request();

    /**
     * Returns the headers of the HTTP request.
     * 
     * @return the headers of the HTTP request
     */
    default HttpHeaders headers() {
        return request().headers();
    }

    /**
     * Returns the trailing headers of the HTTP request.
     * 
     * @return the trailing headers of the HTTP request
     */
    default HttpHeaders trailingHeaders() {
        return request().trailingHeaders();
    }

    /**
     * Returns the {@link QueryStringDecoder} built from the HTTP request
     * {@code URI}.
     * 
     * @return a {@code QueryStringDecoder}
     */
    QueryStringDecoder queryStringDecoder();

    /**
     * Returns the raw path (with query string) of the HTTP request {@code URI}.
     * 
     * @return the raw path string
     */
    default String rawPath() {
        return queryStringDecoder().rawPath();
    }

    /**
     * Returns the raw query string of the HTTP request {@code URI}.
     * 
     * @return the raw query string
     */
    default String rawQuery() {
        return queryStringDecoder().rawQuery();
    }

    /**
     * Returns the decoded key-value parameter pairs of the HTTP request
     * {@code URI}.
     * 
     * @return the decoded key-value parameter pairs
     */
    default Map<String, List<String>> queryParameters() {
        return queryStringDecoder().parameters();
    }

    @Override
    default int refCnt() {
        return request().refCnt();
    }

    @Override
    default HttpRequestContext retain() {
        request().retain();
        return this;
    }

    @Override
    default HttpRequestContext retain(int increment) {
        request().retain(increment);
        return this;
    }

    @Override
    default HttpRequestContext touch() {
        request().touch();
        return this;
    }

    @Override
    default HttpRequestContext touch(Object hint) {
        request().touch(hint);
        return this;
    }

    @Override
    default boolean release() {
        return request().release();
    }

    @Override
    default boolean release(int decrement) {
        return request().release(decrement);
    }

}
