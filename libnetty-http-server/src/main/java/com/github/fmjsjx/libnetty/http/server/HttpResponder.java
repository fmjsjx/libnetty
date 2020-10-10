package com.github.fmjsjx.libnetty.http.server;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.github.fmjsjx.libnetty.http.server.exception.HttpFailureException;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Defines method to respond {@link HttpResponse}s.
 * 
 * @since 1.1
 *
 * @author MJ Fang
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

}