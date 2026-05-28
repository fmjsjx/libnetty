package com.github.fmjsjx.libnetty.http.server;

import com.github.fmjsjx.libnetty.http.HttpCommonUtil;
import com.github.fmjsjx.libnetty.http.server.HttpServer.User;
import com.github.fmjsjx.libnetty.http.server.component.HttpServerComponent;
import com.github.fmjsjx.libnetty.http.server.exception.HttpFailureException;
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.sse.SseEventStreamBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.fmjsjx.libnetty.http.server.Constants.DEFAULT_CHUNK_SIZE;
import static com.github.fmjsjx.libnetty.http.server.HttpServerHandler.READ_NEXT;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.file.StandardOpenOption.READ;

/**
 * A context that runs through each HTTP requests.
 *
 * @author MJ Fang
 * @since 1.1
 */
public interface HttpRequestContext extends ReferenceCounted, HttpResponder {

    /**
     * {@code "text/plain; charset=UTF-8"}
     */
    AsciiString TEXT_PLAIN_UTF8 = HttpCommonUtil.contentType(TEXT_PLAIN, CharsetUtil.UTF_8);

    /**
     * Returns the value of the running Java Virtual Machine's high-resolution time
     * source, in nanoseconds, when the HTTP request just received.
     *
     * @return the value of the running Java Virtual Machine's high-resolution time
     * source, in nanoseconds
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
        return receivedTime().withZoneSameInstant(zone);
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
     * otherwise
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
     * Returns the protocol version of the HTTP/HTTP2 request.
     * <p>
     * Unlike {@link #version()}, this method returns the protocol
     * version string of the HTTP/HTTP2 request. The possible values are:
     * <pre>{@code
     * "HTTP/1.0", "HTTP/1.1", "h2", "h2c"}</pre>
     *
     * @return the protocol version of the HTTP/HTTP2 request
     * @since 4.1
     */
    String protocolVersion();

    /**
     * Returns if the connection is encrypted or not.
     *
     * @return {@code true} if the connection is encrypted, {@code false}
     * otherwise
     * @since 4.1
     */
    boolean sslEnabled();

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
     * Returns the {@code uri} of the HTTP request.
     *
     * @return the {@code uri} string
     */
    default String uri() {
        return queryStringDecoder().uri();
    }

    /**
     * Returns the raw path of the HTTP request {@code URI}.
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
        return queryParameter(name, false);
    }

    /**
     * Returns the value of the specified name belongs to the decoded key-value
     * parameter pairs of the HTTP request {@code URI}
     *
     * @param name                the name of the query parameter
     * @param compatibleWithArray whether the query name is compatible with array style
     * @return an {@code Optional<List<String>>}
     * @since 3.4
     */
    default Optional<List<String>> queryParameter(String name, boolean compatibleWithArray) {
        if (compatibleWithArray) {
            var values = queryParameters().get(name);
            if (values == null) {
                values = queryParameters().get(name.endsWith("[]") ? name.substring(0, name.length() - 2) : name + "[]");
            }
            return Optional.ofNullable(values);
        }
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
    HttpRequestContext pathVariables(PathVariables pathVariables);

    /**
     * Returns the component with the specified {@code componentType}.
     *
     * @param <C>           the type of the component
     * @param componentType the type of the component
     * @return an {@code Optional<HttpServerComponent>}
     */
    <C extends HttpServerComponent> Optional<C> component(Class<? extends C> componentType);

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
     * @return an {@code Optional<T>} may contains the property value
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
     * @return an {@code Optional<T>} may contains the property value
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
     * @return this {@code HttpRequestContext}
     */
    HttpRequestContext property(Object key, Object value);

    /**
     * Put the property value with the key already provided by itself.
     *
     * @param value the value of the property
     * @return this {@code HttpRequestContext}
     */
    default HttpRequestContext putProperty(PropertyKeyProvider value) {
        Objects.requireNonNull(value, "value must not be null");
        return property(value.key(), value);
    }

    /**
     * Returns {@code true} if this {@link HttpRequestContext} contains a property
     * with the specified {@code key}.
     *
     * @param key the key of the property
     * @return {@code true} if this {@code HttpRequestContext} contains a property
     * with the specified {@code key}
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
     * with the specified {@code key} and {@code value}
     */
    default boolean hasProperty(Object key, Object value) {
        return property(key).filter(value::equals).isPresent();
    }

    /**
     * Returns a {@link Stream} contains the key of each property in this
     * {@link HttpRequestContext}.
     *
     * @return a {@code Stream<Object>}
     * @deprecated please use {@link #propertyKeyNames()} instead
     */
    @Deprecated
    default Stream<Object> propertyKeys() {
        return propertyKeyNames().map(Function.identity());
    }

    /**
     * Returns a {@link Stream} contains the key name of each property in the
     * {@link HttpRequestContext}.
     *
     * @return a {@code Stream<String>}
     * @since 1.3
     */
    Stream<String> propertyKeyNames();

    /**
     * Returns the matched route.
     *
     * @return an {@code Optional<T>} may contains the matched route
     * @since 2.6
     */
    default Optional<Router.MatchedRoute> matchedRoute() {
        return property(Router.MatchedRoute.KEY);
    }

    /**
     * {@code "_cookies"}. The key of the {@link Cookie}s used in HTTP request context properties.
     */
    Object PROPERTY_KEY_COOKIES = "_cookies";

    /**
     * Returns the list contains all {@link Cookie}s.
     *
     * @return a {@code Optional<List<Cookie>>}
     * @since 2.7
     */
    default List<Cookie> cookies() {
        return cookies(true);
    }

    /**
     * Returns the list contains all {@link Cookie}s.
     *
     * @param strict if {@code true} then validate name and value chars are in the valid scope defined in {@code RFC6265}
     * @return a {@code Optional<List<Cookie>>}
     * @since 2.7
     */
    default List<Cookie> cookies(boolean strict) {
        Optional<List<Cookie>> cookiesValue = property(PROPERTY_KEY_COOKIES);
        if (cookiesValue.isPresent()) {
            return cookiesValue.get();
        }
        List<Cookie> cookies;
        var cookie = headers().get(HttpHeaderNames.COOKIE);
        if (cookie == null) {
            cookies = List.of();
        } else {
            cookies = (strict ? ServerCookieDecoder.STRICT : ServerCookieDecoder.LAX).decodeAll(cookie);
        }
        property(PROPERTY_KEY_COOKIES, cookies);
        return cookies;
    }

    /**
     * Returns the {@link Cookie} with the specified {@code name} given.
     *
     * @param name the name of the cookie
     * @return an {@code Optional<Cookie>}
     * @since 2.7
     */
    default Optional<Cookie> cookie(String name) {
        return cookies().stream().filter(c -> c.name().equals(name)).findFirst();
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
        if (cause instanceof IllegalArgumentException) {
            return respondBadRequestError(cause);
        }
        return respondInternalServerError(cause);
    }

    @Override
    default CompletableFuture<HttpResult> simpleRespond(HttpFailureException cause) {
        if (cause instanceof ManualHttpFailureException e) {
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
    default CompletableFuture<HttpResult> respondBadRequestError(Throwable cause) {
        HttpResponseStatus status = BAD_REQUEST;
        String message = cause.getMessage();
        String value = message == null ? status.toString()
                : status.code() + " " + status.reasonPhrase() + ": " + message;
        ByteBuf content = alloc().buffer();
        int contentLength = ByteBufUtil.writeUtf8(content, value);
        return simpleRespond(status, content, contentLength, TEXT_PLAIN_UTF8);
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

    @Override
    default CompletableFuture<HttpResult> sendRedirect(CharSequence location) {
        return sendRedirect(location, null);
    }

    @Override
    default CompletableFuture<HttpResult> sendRedirect(CharSequence location, Consumer<HttpHeaders> addHeaders) {
        FullHttpResponse response = responseFactory().createFull(FOUND);
        String rawQuery = rawQuery();
        if (StringUtil.isNullOrEmpty(rawQuery)) {
            response.headers().set(LOCATION, location);
        } else {
            if (location instanceof AsciiString) {
                if (((AsciiString) location).indexOf('?', 0) == -1) {
                    response.headers().set(LOCATION, location + "?" + rawQuery);
                } else {
                    response.headers().set(LOCATION, location);
                }
            } else {
                String base = location.toString();
                if (base.indexOf('?') == -1) {
                    response.headers().set(LOCATION, base + "?" + rawQuery);
                } else {
                    response.headers().set(LOCATION, base);
                }
            }
        }
        if (addHeaders != null) {
            addHeaders.accept(response.headers());
        }
        return sendResponse(response, 0);
    }

    @Override
    default CompletableFuture<HttpResult> sendFile(Path filePath, Consumer<HttpHeaders> addHeaders) {
        try {
            var fileSize = Files.size(filePath);
            var response = responseFactory().create(OK);
            HttpUtil.setContentLength(response, fileSize);
            var headers = response.headers();
            if (!headers.contains(CACHE_CONTROL)) {
                headers.set(CACHE_CONTROL, NO_CACHE);
            }
            var now = new Date();
            headers.set(DATE, now);
            headers.set(EXPIRES, now);
            var future = new CompletableFuture<HttpResult>();
            var callbacks = new ChannelFutureListener[]{channelFuture -> {
                if (channelFuture.isSuccess()) {
                    future.complete(new DefaultHttpResult(this, fileSize, OK));
                } else if (channelFuture.cause() != null) {
                    future.completeExceptionally(channelFuture.cause());
                }
            }, isKeepAlive() ? READ_NEXT : CLOSE};
            var channel = channel();
            var useZeroCopy = !sslEnabled() && !(channel instanceof Http2StreamChannel);
            if (useZeroCopy) {
                // disable compression feature when use zero-copy
                response.headers().set(CONTENT_ENCODING, IDENTITY);
            }
            channel.write(response);
            FileChannel file = FileChannel.open(filePath, READ);
            if (useZeroCopy) {
                channel.write(new DefaultFileRegion(file, 0, fileSize));
                // Write the end marker.
                channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListeners(callbacks);
            } else {
                var chunkedFile = new ChunkedNioFile(file, 0, fileSize, DEFAULT_CHUNK_SIZE);
                // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                channel.writeAndFlush(new HttpChunkedInput(chunkedFile)).addListeners(callbacks);
            }
            return future;
        } catch (IOException e) {
            // Just respond an error when an I/O error occurs
            return respondInternalServerError(e);
        }
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

    /**
     * Provides the key of the property in HTTP request context.
     *
     * @since 2.6
     */
    interface PropertyKeyProvider {

        /**
         * Returns the key.
         *
         * @return the key
         */
        Object key();

    }

    /**
     * Creates and returns a new {@link SseEventStreamBuilder}.
     * <p>
     * This method is equivalent to: <pre>{@code
     * SseEventStreamBuilder.create(this);
     * }</pre>
     *
     * @return a new {@link SseEventStreamBuilder}
     * @see SseEventStreamBuilder#create(HttpRequestContext)
     * @since 3.9
     */
    default SseEventStreamBuilder eventStreamBuilder() {
        return SseEventStreamBuilder.create(this);
    }

}
