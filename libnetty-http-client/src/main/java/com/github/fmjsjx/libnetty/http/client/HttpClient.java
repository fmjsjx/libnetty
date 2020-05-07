package com.github.fmjsjx.libnetty.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import com.github.fmjsjx.libnetty.http.client.exception.HttpRuntimeException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;

/**
 * Main interface for an HTTP client.
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
public interface HttpClient extends AutoCloseable {

    /**
     * Returns the SSL context of this {@link HttpClient}.
     * 
     * @return the {@link SSLContext} of this {@link HttpClient}
     */
    SslContext sslContext();

    /**
     * Close this HTTP client.
     */
    @Override
    default void close() {
        // default do nothing
    }

    /**
     * Sends the given request asynchronously using this client with the given
     * response content handler.
     * 
     * <p>
     * <b>Note: The returned {@link CompletableFuture} will run on the netty
     * threads!</b>
     * 
     * @param <T>            the response content type
     * @param request        the request
     * @param contentHandler the response content handler
     * @return a {@code CompletableFuture<HttpClient.Response<T>>}
     */
    <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler);

    /**
     * Sends the given request asynchronously using this client with the given
     * response content handler.
     * 
     * @param <T>            the response content type
     * @param request        the request
     * @param contentHandler the response content handler
     * @param executor       the executor to use for asynchronous execution
     * @return a {@code CompletableFuture<HttpClient.Response<T>>}
     */
    <T> CompletableFuture<Response<T>> sendAsync(Request request, HttpContentHandler<T> contentHandler,
            Executor executor);

    /**
     * Sends the given request using this client, blocking if necessary to get the
     * response.
     * 
     * @param <T>            the response content type
     * @param request        the request
     * @param contentHandler the response content handler
     * @return the response
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     * @throws HttpRuntimeException if any other error occurs
     */
    default <T> Response<T> send(Request request, HttpContentHandler<T> contentHandler)
            throws IOException, InterruptedException, HttpRuntimeException {
        try {
            return sendAsync(request, contentHandler).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new HttpRuntimeException(cause);
            }
        }
    }

    /**
     * HTTP request.
     * 
     * @since 1.0
     * 
     * @author fmjsjx
     */
    interface Request {

        /**
         * Returns the HTTP method of this {@link Request}.
         *
         * @return the {@link HttpMethod} of this {@link Request}
         */
        HttpMethod method();

        /**
         * Returns the URI of this {@link Request}.
         * 
         * @return the {@link URI} of this {@link Request}
         */
        URI uri();

        /**
         * Returns the HTTP headers of this {@link Request}.
         * 
         * @return the {@link HttpHeaders} of this {@link Request}
         */
        HttpHeaders headers();

        /**
         * Returns the trailing HTTP headers of this {@link Request}.
         * 
         * @return the trailing {@link HttpHeaders} of this {@link Request}
         */
        HttpHeaders trailingHeaders();

        /**
         * Returns the content of this {@link Request}.
         * 
         * @return the {@link ByteBuf} content of this {@link Request}
         */
        ByteBuf content();

    }

    /**
     * Builder for {@link Request}
     * 
     * @since 1.0
     * 
     * @author fmjsjx
     */
    @SuppressWarnings("unchecked")
    abstract class RequestBuilder<Self extends RequestBuilder<?>> {
        
        protected HttpMethod method;
        protected URI uri;
        protected HttpHeaders headers;
        protected HttpHeaders trailingHeaders;
        protected ByteBuf content;
        
        public Self uri(URI uri) {
            this.uri = Objects.requireNonNull(uri, "uri must not be null");
            return (Self) this;
        }
        
        public Self method(HttpMethod method, ByteBuf content) {
            this.method = Objects.requireNonNull(method, "method must not be null");
            this.content = content == null ? Unpooled.EMPTY_BUFFER : content;
            return (Self) this;            
        }
        
        public Self get() {
            return method(HttpMethod.GET, null);
        }
        
        public Self options() {
            return method(HttpMethod.OPTIONS, null);
        }
        
        // TODO

        public Request build() {
            // TODO
            return build0();
        }
        
        protected abstract Request build0();
        
    }

    /**
     * HTTP response.
     *
     * @param <T> Type of response content
     * 
     * @since 1.0
     * 
     * @author fmjsjx
     */
    interface Response<T> {

        /**
         * Returns the protocol version of this {@link Response}.
         * 
         * @return the {@link HttpVersion} of this {@link Response}
         */
        HttpVersion version();

        /**
         * Returns the HTTP status of this {@link Response}.
         * 
         * @return the {@link HttpResponseStatus} of this {@link Response}
         */
        HttpResponseStatus status();

        /**
         * Returns the code of this {@link Response}.
         * 
         * @return the code of this {@link Response}
         */
        default int statusCode() {
            return status().code();
        }

        /**
         * Returns the headers of this {@link Response}.
         * 
         * @return the {@link HttpHeaders} of this {@link Response}
         */
        HttpHeaders headers();

        /**
         * Returns the value of a header with the specified name. If there are more than
         * one values for the specified name, the first value is returned.
         * 
         * @param name The name of the header to search
         * @return the {@link Optional} instance contains the first header value or
         *         {@link Optional#empty()} if there is no such header
         */
        default Optional<String> header(CharSequence name) {
            return Optional.ofNullable(headers().get(name));
        }

        /**
         * Returns the integer value of a header with the specified name. If there are
         * more than one values for the specified name, the first value is returned.
         * 
         * @param name the name of the header to search
         * @return the {@link OptionalInt} instance contains the first header value if
         *         the header is found and its value is an integer.
         *         {@link OptionalInt#empty()} if there's no such header or its value is
         *         not an integer.
         */
        default OptionalInt intHeader(CharSequence name) {
            Integer value = headers().getInt(name);
            return value == null ? OptionalInt.empty() : OptionalInt.of(value.intValue());
        }

        /**
         * Returns the content of this {@link Response}.
         * 
         * @return the content of this {@link Response}.
         */
        T content();

    }

}
