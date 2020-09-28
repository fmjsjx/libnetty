package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * Utility class for HTTP response.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class HttpResponseUtil {

    /**
     * Creates and returns a new no content {@link FullHttpResponse} by the
     * specified parameters.
     * 
     * @param version   the version of HTTP
     * @param status    the status of the response
     * @param keepAlive if the connection is {@code keep-alive} or not
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, Unpooled.EMPTY_BUFFER);
        HttpHeaders headers = response.headers();
        HttpUtil.setKeepAlive(headers, version, keepAlive);
        headers.set(CONTENT_LENGTH, ZERO);
        return response;
    }

    /**
     * Creates and returns a new no content {@link FullHttpResponse} by the
     * specified parameters.
     * 
     * @param version    the version of HTTP
     * @param status     the status of the response
     * @param keepAlive  if the connection is {@code keep-alive} or not
     * @param addHeaders function to add headers
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, boolean keepAlive,
            Consumer<HttpHeaders> addHeaders) {
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, Unpooled.EMPTY_BUFFER);
        HttpHeaders headers = response.headers();
        addHeaders.accept(headers);
        HttpUtil.setKeepAlive(headers, version, keepAlive);
        headers.set(CONTENT_LENGTH, ZERO);
        return response;
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} by the specified
     * parameters.
     * 
     * @param version       the version of HTTP
     * @param status        the status of the response
     * @param content       the content of the response body
     * @param contentLength the length of the response body content
     * @param contentType   the type of the response body content
     * @param keepAlive     if the connection is {@code keep-alive} or not
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, ByteBuf content,
            long contentLength, CharSequence contentType, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, content);
        HttpHeaders headers = response.headers();
        HttpUtil.setKeepAlive(headers, version, keepAlive);
        headers.set(CONTENT_LENGTH, contentLength);
        headers.set(CONTENT_TYPE, contentType);
        return response;
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} by the specified
     * parameters.
     * 
     * @param version       the version of HTTP
     * @param status        the status of the response
     * @param content       the content of the response body
     * @param contentLength the length of the response body content
     * @param contentType   the type of the response body content
     * @param keepAlive     if the connection is {@code keep-alive} or not
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, ByteBuf content,
            int contentLength, CharSequence contentType, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, content);
        HttpHeaders headers = response.headers();
        HttpUtil.setKeepAlive(headers, version, keepAlive);
        headers.setInt(CONTENT_LENGTH, contentLength);
        headers.set(CONTENT_TYPE, contentType);
        return response;
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} by the specified
     * parameters.
     * 
     * @param version     the version of HTTP
     * @param status      the status of the response
     * @param content     the content of the response body
     * @param contentType the type of the response body content
     * @param keepAlive   if the connection is {@code keep-alive} or not
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, ByteBuf content,
            CharSequence contentType, boolean keepAlive) {
        return create(version, status, content, content.readableBytes(), contentType, keepAlive);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} by the specified
     * parameters.
     * 
     * @param version   the version of HTTP
     * @param status    the status of the response
     * @param alloc     the allocator to allocate {@link ByteBuf}s
     * @param keepAlive if the connection is {@code keep-alive} or not
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, ByteBufAllocator alloc,
            boolean keepAlive) {
        return create(version, status, alloc, CharsetUtil.UTF_8, keepAlive);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} by the specified
     * parameters.
     * 
     * @param version   the version of HTTP
     * @param status    the status of the response
     * @param alloc     the allocator to allocate {@link ByteBuf}s
     * @param charset   the character set of the response body content
     * @param keepAlive if the connection is {@code keep-alive} or not
     * 
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse create(HttpVersion version, HttpResponseStatus status, ByteBufAllocator alloc,
            Charset charset, boolean keepAlive) {
        byte[] b = status.toString().getBytes();
        ByteBuf content = alloc.buffer(b.length, b.length).writeBytes(b);
        return create(version, status, content, b.length, contentType(TEXT_PLAIN, charset), keepAlive);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} with status
     * {@code "200 OK"} by the specified parameters.
     * 
     * @param version       the version of HTTP
     * @param content       the content of the response body
     * @param contentLength the length of the response body content
     * @param contentType   the content type of the response body
     * @param keepAlive     if the connection is {@code keep-alive} or not
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse ok(HttpVersion version, ByteBuf content, long contentLength,
            CharSequence contentType, boolean keepAlive) {
        return create(version, OK, content, contentLength, contentType, keepAlive);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} with status
     * {@code "200 OK"} by the specified parameters.
     * 
     * @param version       the version of HTTP
     * @param content       the content of the response body
     * @param contentLength the length of the response body content
     * @param contentType   the content type of the response body
     * @param keepAlive     if the connection is {@code keep-alive} or not
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse ok(HttpVersion version, ByteBuf content, int contentLength,
            CharSequence contentType, boolean keepAlive) {
        return create(version, OK, content, contentLength, contentType, keepAlive);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} with status
     * {@code "200 OK"} by the specified parameters.
     * 
     * @param version     the version of HTTP
     * @param content     the content of the response body
     * @param contentType the content type of the response body
     * @param keepAlive   if the connection is {@code keep-alive} or not
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse ok(HttpVersion version, ByteBuf content, CharSequence contentType,
            boolean keepAlive) {
        return ok(version, content, content.readableBytes(), contentType, keepAlive);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} with {@code "302 Found"}
     * status by the specified parameters.
     * 
     * @param version   the version of HTTP
     * @param status    the status of the response
     * @param keepAlive if the connection is {@code keep-alive} or not
     * @param location  the URI to be redirect to
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse redirect(HttpVersion version, boolean keepAlive, CharSequence location) {
        return redirect(version, FOUND, keepAlive, location);
    }

    /**
     * Creates and returns a new {@link FullHttpResponse} with {@code 3XX} status by
     * the specified parameters.
     * 
     * @param version   the version of HTTP
     * @param status    the status of the response
     * @param keepAlive if the connection is {@code keep-alive} or not
     * @param location  the URI to be redirect to
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse redirect(HttpVersion version, HttpResponseStatus status, boolean keepAlive,
            CharSequence location) {
        FullHttpResponse response = create(version, status, keepAlive);
        response.headers().add(LOCATION, location);
        return response;
    }

    private static final class BadRequest {

        private static final ByteBufAllocator ALLOC = UnpooledByteBufAllocator.DEFAULT;

        private static final int contentLength = BAD_REQUEST.toString().getBytes().length;

        private static final ByteBuf content = Unpooled.unreleasableBuffer(
                ALLOC.buffer(contentLength, contentLength).writeBytes(BAD_REQUEST.toString().getBytes()).asReadOnly());

        private static final CharSequence contentType = contentType(TEXT_PLAIN, CharsetUtil.UTF_8);

    }

    /**
     * Creates and returns a new {@link FullHttpResponse} with
     * {@code "400 Bad Request"} status.
     * 
     * @param version   the version of HTTP
     * @param keepAlive if the connection is {@code keep-alive} or not
     * @return a {@code FullHttpResponse}
     */
    public static final FullHttpResponse badRequest(HttpVersion version, boolean keepAlive) {
        return create(version, BAD_REQUEST, BadRequest.content.duplicate(), BadRequest.contentLength,
                BadRequest.contentType, keepAlive);
    }

    private HttpResponseUtil() {
    }

}
