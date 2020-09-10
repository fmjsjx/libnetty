package com.github.fmjsjx.libnetty.http.server;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate
     * {@link ByteBuf}s.
     * 
     * @return a {@code ByteBufAllocator}
     */
    default ByteBufAllocator alloc() {
        return channel().alloc();
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
     * Returns the content body of the HTTP request.
     * 
     * @return the content body of the HTTP request
     */
    default ByteBuf body() {
        return request().content();
    }

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

    /**
     * Returns the property value as parameterized type.
     * 
     * @param <T> the type of the property value
     * @param key the key of the property
     * 
     * @return an {@code Optional<T>} may contains the property value
     * 
     * @throws ClassCastException if the object is not {@code null} and is not
     *                            assignable to the type {@code T}
     */
    <T> Optional<T> property(Object key) throws ClassCastException;

    /**
     * Returns the property value as parameterized type.
     * 
     * @param <T>  the type of the property value
     * @param key  the key of the property
     * @param type the class of the type
     * 
     * @return an {@code Optional<T>} may contains the property value
     * 
     * @throws ClassCastException if the object is not {@code null} and is not
     *                            assignable to the type {@code T}
     */
    <T> Optional<T> property(Object key, Class<T> type) throws ClassCastException;

    /**
     * Set the property value with the specified key, or remove the value with
     * specified key by input {@code null} parameter.
     * 
     * @param key   the key of the property
     * @param value the value of the property
     * 
     * @return this {@code HttpRequestContext}
     */
    HttpRequestContext property(Object key, Object value);

    /**
     * Returns {@code true} if this {@link HttpRequestContext} contains a property
     * with the specified {@code key}.
     * 
     * @param key the key of the property
     * @return {@code true} if this {@code HttpRequestContext} contains a property
     *         with the specified {@code key}
     */
    default boolean hasProperty(Object key) {
        return property(key).isPresent();
    }

    /**
     * Returns {@code true} if this {@link HttpRequestContext} contains a property
     * with the specified {@code key} and {@code value}.
     * 
     * @param key   the key of the property
     * @param value the value of the property
     * @return {@code true} if this {@code HttpRequestContext} contains a property
     *         with the specified {@code key} and {@code value}
     */
    default boolean hasProperty(Object key, Object value) {
        return property(key).filter(value::equals).isPresent();
    }

    /**
     * Returns a {@link Stream} contains the key of each property in this
     * {@link HttpRequestContext}.
     * 
     * @return a {@code Stream<Object>}
     */
    Stream<Object> propertyKeys();

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
