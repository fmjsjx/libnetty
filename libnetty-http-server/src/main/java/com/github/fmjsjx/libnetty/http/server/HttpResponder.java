package com.github.fmjsjx.libnetty.http.server;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.github.fmjsjx.libnetty.http.server.exception.HttpFailureException;

import com.github.fmjsjx.libnetty.http.server.util.MimeTypesUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * Defines method to respond {@link HttpResponse}s.
 *
 * @author MJ Fang
 * @since 1.1
 */
public interface HttpResponder {

    /**
     * Respond a simple HTTP response without content to client and returns the
     * {@link HttpResult} asynchronously.
     *
     * @param status the status
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status);

    /**
     * Respond a simple HTTP response without content to client and returns the
     * {@link HttpResult} asynchronously.
     *
     * @param status     the status
     * @param addHeaders a function to add headers
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status, Consumer<HttpHeaders> addHeaders);

    /**
     * Respond HTTP error response to client and returns the {@link HttpResult}
     * asynchronously.
     *
     * @param cause the cause
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> respondError(Throwable cause);

    /**
     * Respond a simple HTTP response with error message content to client and
     * returns the {@link HttpResult} asynchronously.
     *
     * @param cause a {@code HttpFailureException}
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> simpleRespond(HttpFailureException cause);

    /**
     * Respond a simple HTTP response with the specified content to client and
     * returns the {@link HttpResult} asynchronously.
     *
     * @param status      the status
     * @param content     the content
     * @param contentType the type of the content
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status, ByteBuf content, CharSequence contentType);

    /**
     * Respond a simple HTTP response with the specified content to client and
     * returns the {@link HttpResult} asynchronously.
     *
     * @param status        the status
     * @param content       the content
     * @param contentLength the length of the content
     * @param contentType   the type of the content
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> simpleRespond(HttpResponseStatus status, ByteBuf content, int contentLength,
                                                CharSequence contentType);

    /**
     * Send HTTP response with {@code "400 Bad Request Error"} to client and returns
     * the {@link HttpResult} asynchronously.
     *
     * @param cause the cause
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> respondBadRequestError(Throwable cause);

    /**
     * Send HTTP response with {@code "500 Internal Server Error"} to client and
     * returns the {@link HttpResult} asynchronously.
     *
     * @param cause the cause
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> respondInternalServerError(Throwable cause);

    /**
     * Send HTTP response to client and returns the HttpResultasynchronously.
     *
     * @param response      the FullHttpResponse
     * @param contentLength the length of the response body content
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> sendResponse(FullHttpResponse response, int contentLength);

    /**
     * Send HTTP response to client and returns the HttpResultasynchronously.
     *
     * @param response the FullHttpResponse
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> sendResponse(FullHttpResponse response);

    /**
     * Send HTTP response with {@code "302 Found"} to client and returns the
     * {@link HttpResult} asynchronously.
     *
     * @param location the location
     * @return a {@code CompletableFuture<HttpResult>}
     * @since 1.2
     */
    CompletableFuture<HttpResult> sendRedirect(CharSequence location);

    /**
     * Send HTTP response with {@code "302 Found"} to client and returns the
     * {@link HttpResult} asynchronously.
     *
     * @param location   the location
     * @param addHeaders a function to add headers
     * @return a {@code CompletableFuture<HttpResult>}
     * @since 1.2
     */
    CompletableFuture<HttpResult> sendRedirect(CharSequence location, Consumer<HttpHeaders> addHeaders);

    /**
     * Send HTTP response with {@code "200 OK"} and file content to client
     * and returns the {@link HttpResult} asynchronously.
     *
     * @param filePath   the file path
     * @param addHeaders a function to add headers
     * @return a {@code CompletableFuture<HttpResult>}
     * {@link HttpResult} asynchronously.
     * @since 4.2
     */
    CompletableFuture<HttpResult> sendFile(Path filePath, Consumer<HttpHeaders> addHeaders);

    /**
     * Send HTTP response with {@code "200 OK"} and file content to client
     * and returns the {@link HttpResult} asynchronously.
     *
     * @param filePath    the file path
     * @param contentType the content type
     * @return a {@code CompletableFuture<HttpResult>}
     * @since 4.2
     */
    default CompletableFuture<HttpResult> sendFile(Path filePath, CharSequence contentType) {
        return sendFile(filePath, headers -> headers.set(CONTENT_TYPE, contentType));
    }

    /**
     * Send HTTP response with {@code "200 OK"} and file content to client
     * and returns the {@link HttpResult} asynchronously.
     *
     * @param filePath the file path
     * @return a {@code CompletableFuture<HttpResult>}
     * @since 4.2
     */
    default CompletableFuture<HttpResult> sendFile(Path filePath) {
        return sendFile(filePath, MimeTypesUtil.probeContentType(filePath));
    }

}