package com.github.fmjsjx.libnetty.http.server;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.fmjsjx.libnetty.http.server.HttpServer.User;
import com.github.fmjsjx.libnetty.http.server.exception.HttpFailureException;
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCounted;

/**
 * A context that runs through each HTTP requests.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface HttpRequestContext extends ReferenceCounted, HttpResponder {

    /**
     * {@code "text/plain; charset=UTF-8"}
     */
    AsciiString TEXT_PLAIN_UTF8 = com.github.fmjsjx.libnetty.http.HttpUtil.contentType(TEXT_PLAIN, CharsetUtil.UTF_8);

    /**
     * Returns the value of the running Java Virtual Machine'shigh-resolution time
     * source, in nanoseconds, when the HTTP request just received.
     * 
     * @return the value of the running Java Virtual Machine'shigh-resolution time
     *         source, in nanoseconds
     * @see System#nanoTime()
     */
    long receivedNanoTime();

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
     * Returns if the connection is {@code keep-alive} or not.
     * 
     * @return {@code true} if the connection is {@code keep-alive}, {@code false}
     *         otherwise
     */
    default boolean isKeepAlive() {
        return HttpUtil.isKeepAlive(request());
    }

    /**
     * Returns the protocol version of the HTTP request.
     * 
     * @return the protocol version of the HTTP request
     */
    default HttpVersion version() {
        return request().protocolVersion();
    }

    /**
     * Returns the method of the HTTP request.
     * 
     * @return the method of the HTTP request
     */
    default HttpMethod method() {
        return request().method();
    }

    /**
     * Returns the content body of the HTTP request.
     * 
     * @return the content body of the HTTP request
     */
    default ByteBuf body() {
        return request().content();
    }

    /**
     * Returns the length of the HTTP request body content.
     * 
     * @return the length of the HTTP request body content
     */
    int contentLength();

    /**
     * Returns the headers of the HTTP request.
     * 
     * @return the headers of the HTTP request
     */
    default HttpHeaders headers() {
        return request().headers();
    }

    /**
     * Returns the type of the HTTP request body content.
     * 
     * @return the type of the HTTP request body content
     */
    Optional<CharSequence> contentType();

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
     * Returns the decoded path string of the HTTP request {@code URI}.
     * 
     * @return the decoded path string
     */
    default String path() {
        return queryStringDecoder().path();
    }

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
     * Returns the value of the specified name belongs to the decoded key-value
     * parameter pairs of the HTTP request {@code URI}
     * 
     * @param name the name of the query parameter
     * @return an {@code Optional<List<String>>}
     */
    default Optional<List<String>> queryParameter(String name) {
        return Optional.ofNullable(queryParameters().get(name));
    }

    /**
     * Returns the path variables.
     * 
     * @return a {@code PathVariables}
     */
    PathVariables pathVariables();

    /**
     * Set the path variables.
     * 
     * @param pathVariables the path variables
     * @return this {@code HttpRequestContext}
     */
    HttpResponder pathVariables(PathVariables pathVariables);

    /**
     * Returns the {@link User}.
     * 
     * @return an {@code Optional<T>} may contains the user
     */
    default Optional<User> user() {
        return user(User.class);
    }

    /**
     * Returns the {@link User}.
     * 
     * @param <U>  the real type of the user
     * @param type the class of the real type
     * @return an {@code Optional<U extends User>} may contains the user
     * 
     * @throws ClassCastException if the user is not {@code null} and is not
     *                            assignable to the type {@code U}
     */
    default <U extends User> Optional<U> user(Class<U> type) throws ClassCastException {
        return property(User.KEY, type);
    }

    /**
     * Returns the property value as parameterized type.
     * 
     * @param <T> the type of the property value
     * @param key the key of the property, also the class of the type
     * 
     * @return an {@code Optional<T>} may contains the property value
     */
    default <T> Optional<T> property(Class<T> key) {
        return property(key, key);
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
    HttpResponder property(Object key, Object value);

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

    @Override
    default CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status) {
        return simpleRespond(status, null);
    }

    @Override
    default CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status, Consumer<HttpHeaders> addHeaders) {
        FullHttpResponse response = responseFactory().createFull(status);
        if (addHeaders != null) {
            addHeaders.accept(response.headers());
        }
        return sendResponse(response, 0);
    }

    @Override
    default CompletableFuture<HttpResult> respondError(Throwable cause) {
        if (cause instanceof HttpFailureException) {
            return simpleRespond((HttpFailureException) cause);
        }
        return respondInternalServerError(cause);
    }

    @Override
    default CompletableFuture<HttpResult> simpleRespond(HttpFailureException cause) {
        if (cause instanceof ManualHttpFailureException) {
            ManualHttpFailureException e = (ManualHttpFailureException) cause;
            ByteBuf content = alloc().buffer();
            int contentLength = ByteBufUtil.writeUtf8(content, e.content());
            return simpleRespond(e.status(), content, contentLength, e.contentType());
        }
        ByteBuf content = alloc().buffer();
        int contentLength = ByteBufUtil.writeUtf8(content, cause.getLocalizedMessage());
        return simpleRespond(cause.status(), content, contentLength, TEXT_PLAIN_UTF8);
    }

    @Override
    default CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status, ByteBuf content,
            CharSequence contentType) {
        return simpleRespond(status, content, content.readableBytes(), contentType);
    }

    @Override
    default CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status, ByteBuf content, int contentLength,
            CharSequence contentType) {
        return sendResponse(responseFactory().createFull(status, content, contentLength, contentType), contentLength);
    }

    @Override
    default CompletableFuture<HttpResult> respondInternalServerError(Throwable cause) {
        HttpResponseStatus status = INTERNAL_SERVER_ERROR;
        String value = status.code() + " " + status.reasonPhrase() + ": " + cause.toString();
        ByteBuf content = alloc().buffer();
        int contentLength = ByteBufUtil.writeUtf8(content, value);
        return simpleRespond(status, content, contentLength, TEXT_PLAIN_UTF8);
    }

    @Override
    default CompletableFuture<HttpResult> sendResponse(FullHttpResponse response, int contentLength) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();
        ChannelFuture sendFuture = channel().writeAndFlush(response);
        sendFuture.addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(this, contentLength, response.status()));
            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });
        if (!isKeepAlive()) {
            sendFuture.addListener(CLOSE);
        }
        return future;
    }

    @Override
    default CompletableFuture<HttpResult> sendResponse(FullHttpResponse response) {
        return sendResponse(response, response.content().readableBytes());
    }

    /**
     * Returns the factory creates {@link HttpResponse}s.
     * 
     * @return the factory creates {@link HttpResponse}s
     */
    HttpResponseFactory responseFactory();

    /**
     * A factory to create {@link HttpResponse}s.
     * 
     * @author MJ Fang
     */
    interface HttpResponseFactory {

        /**
         * Creates a new {@link HttpResponse} instance with the specified status.
         * 
         * @param status the status
         * @return a {@code HttpResponse}
         */
        HttpResponse create(HttpResponseStatus status);

        /**
         * Creates a new {@link FullHttpResponse} instance with the specified status and
         * {@code EMPTY} content.
         * 
         * @param status the status
         * @return a {@code FullHttpResponse}
         */
        FullHttpResponse createFull(HttpResponseStatus status);

        /**
         * Creates a new {@link FullHttpResponse} instance with the specified status and
         * content.
         * 
         * @param status      the status
         * @param content     a {@link ByteBuf} contains response body
         * @param contentType the MIME type of the content
         * @return a {@code FullHttpResponse}
         */
        default FullHttpResponse createFull(HttpResponseStatus status, ByteBuf content, CharSequence contentType) {
            return createFull(status, content, content.readableBytes(), contentType);
        }

        /**
         * Creates a new {@link FullHttpResponse} instance with the specified status and
         * content.
         * 
         * @param status        the status
         * @param content       a {@link ByteBuf} contains response body
         * @param contentLength the length of the content
         * @param contentType   the MIME type of the content
         * @return a {@code FullHttpResponse}
         */
        FullHttpResponse createFull(HttpResponseStatus status, ByteBuf content, int contentLength,
                CharSequence contentType);

        /**
         * Creates a new {@link FullHttpResponse} instance with the specified status and
         * the content be same with status as {@code text/plain}.
         * 
         * @param status the status
         * @return a {@code FullHttpResponse}
         */
        default FullHttpResponse createFullText(HttpResponseStatus status) {
            return createFullText(status, CharsetUtil.UTF_8);
        }

        /**
         * Creates a new {@link FullHttpResponse} instance with the specified status and
         * the content be same with status as {@code text/plain}.
         * 
         * @param status  the status
         * @param charset the character-set of the content
         * @return a {@code FullHttpResponse}
         */
        FullHttpResponse createFullText(HttpResponseStatus status, Charset charset);

    }

}
