package com.github.fmjsjx.libnetty.http.client;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import com.github.fmjsjx.libnetty.http.HttpUtil;
import com.github.fmjsjx.libnetty.http.client.exception.HttpRuntimeException;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;

/**
 * Main interface for an HTTP client.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 * 
 * @see ConnectionCachedHttpClient
 * @see SimpleHttpClient
 */
public interface HttpClient extends AutoCloseable {

    /**
     * Returns a new {@link HttpClient} built by the default {@link Builder}.
     * 
     * @return a {@code HttpClient}
     */
    static HttpClient build() {
        return defaultBuilder().build();
    }

    /**
     * Returns a default {@link Builder}.
     * 
     * @return a {@code Builder}
     * @see ConnectionCachedHttpClient#builder()
     */
    static Builder defaultBuilder() {
        return ConnectionCachedHttpClient.builder();
    }

    /**
     * Returns a {@link SimpleHttpClient.Builder}.
     * 
     * @return a {@code Builder}
     * @see SimpleHttpClient#builder()
     */
    static Builder simpleBuilder() {
        return SimpleHttpClient.builder();
    }

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
     * Returns a new {@link ClientWrappedRequestBuilder}.
     * 
     * @return a {@code ClientWrappedRequestBuilder}
     */
    default ClientWrappedRequestBuilder<?> request() {
        return new DefaultRequest.Builder(this);
    }

    /**
     * Returns a new {@link ClientWrappedRequestBuilder} with the specific
     * {@code URI}.
     * 
     * @param uri the {@link URI}
     * @return a {@code ClientWrappedRequestBuilder}
     */
    default ClientWrappedRequestBuilder<?> request(URI uri) {
        return request().uri(uri);
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
     * @author MJ Fang
     */
    interface Request {

        /**
         * Returns a HTTP request builder with the specific {@code URI}.
         * 
         * @param uri the {@link URI}
         * 
         * @return a {@link RequestBuilder}
         */
        static RequestBuilder<?> builder(URI uri) {
            return new DefaultRequest.Builder().uri(uri);
        }

        /**
         * Returns a HTTP request builder.
         * 
         * @return a {@link RequestBuilder}
         */
        static RequestBuilder<?> builder() {
            return new DefaultRequest.Builder();
        }

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
         * Returns the {@link HttpContentHolder}.
         * 
         * @return a {@code HttpContentHolder}
         */
        HttpContentHolder<?> contentHolder();

    }

    /**
     * Builder for {@link Request}
     * 
     * @since 1.0
     * 
     * @author MJ Fang
     */
    @SuppressWarnings("unchecked")
    abstract class RequestBuilder<Self extends RequestBuilder<?>> {

        protected HttpMethod method;
        protected URI uri;
        protected HttpHeaders headers;
        protected HttpHeaders trailingHeaders;
        protected HttpContentHolder<?> contentHolder;

        /**
         * Sets the {@code URI} for this request.
         * 
         * @param uri the {@link URI}
         * @return this builder
         */
        public Self uri(URI uri) {
            this.uri = Objects.requireNonNull(uri, "uri must not be null");
            return (Self) this;
        }

        /**
         * Sets the HTTP method and content for this request.
         * 
         * @param method        the {@code HttpMethod}
         * @param contentHolder the {@code HttpContentHolder}
         * @return this builder
         */
        protected Self method(HttpMethod method, HttpContentHolder<?> contentHolder) {
            this.method = Objects.requireNonNull(method, "method must not be null");
            this.contentHolder = contentHolder == null ? HttpContentHolders.ofEmpty() : contentHolder;
            return (Self) this;
        }

        /**
         * Returns a new HTTP GET request built from the current state of this builder.
         * 
         * @return a {@link Request}
         */
        public Request get() {
            return method(HttpMethod.GET, null).build();
        }

        /**
         * Returns a new HTTP OPTIONS request built from the current state of this
         * builder.
         * 
         * @return this builder
         */
        public Request options() {
            return method(HttpMethod.OPTIONS, null).build();
        }

        /**
         * Returns a new HTTP HEAD request built from the current state of this builder.
         * 
         * @return this builder
         */
        public Request head() {
            return method(HttpMethod.HEAD, null).build();
        }

        /**
         * Returns a new HTTP TRACE request built from the current state of this
         * builder.
         * 
         * @return this builder
         */
        public Request trace() {
            return method(HttpMethod.TRACE, null).build();
        }

        /**
         * Returns a new HTTP POST request built from the current state of this builder.
         * 
         * @param contentHolder the {@code HttpContentHolder}
         * @return this builder
         */
        public Request post(HttpContentHolder<?> contentHolder) {
            return method(HttpMethod.POST, contentHolder).build();
        }

        /**
         * Returns a new HTTP PUT request built from the current state of this builder.
         * 
         * @param contentHolder the {@code HttpContentHolder}
         * @return this builder
         */
        public Request put(HttpContentHolder<?> contentHolder) {
            return method(HttpMethod.PUT, contentHolder).build();
        }

        /**
         * Returns a new HTTP PATCH request built from the current state of this
         * builder.
         * 
         * @param contentHolder the {@code HttpContentHolder}
         * @return this builder
         */
        public Request patch(HttpContentHolder<?> contentHolder) {
            return method(HttpMethod.PATCH, contentHolder).build();
        }

        /**
         * Returns a new HTTP DELETE request built from the current state of this
         * builder.
         * 
         * @param contentHolder the {@code HttpContentHolder}
         * @return this builder
         */
        public Request delete(HttpContentHolder<?> contentHolder) {
            return method(HttpMethod.DELETE, contentHolder).build();
        }

        /**
         * Ensure the HTTP headers for this request.
         * 
         * @return this builder
         */
        protected HttpHeaders ensureHeaders() {
            if (headers == null) {
                headers = new DefaultHttpHeaders();
            }
            return headers;
        }

        /**
         * Adds a new header with the specified name and value for this request.
         * 
         * @param name  the name of the header being added
         * @param value the value of the header being added
         * @return this builder
         */
        public Self header(CharSequence name, Object value) {
            ensureHeaders().add(name, value);
            return (Self) this;
        }

        /**
         * Adds a new header with the specified name and value for this request.
         * 
         * @param name  the name of the header being added
         * @param value the value of the header being added
         * @return this builder
         */
        public Self header(CharSequence name, int value) {
            ensureHeaders().addInt(name, value);
            return (Self) this;
        }

        /**
         * Adds a new header with the specified name and values for this request.
         * 
         * @param name   the name of the header being added
         * @param values the values of the header being added
         * @return this builder
         */
        public Self header(CharSequence name, Iterable<?> values) {
            ensureHeaders().add(name, values);
            return (Self) this;
        }

        /**
         * Sets a header with the specified name and value for this request.
         * 
         * @param name  the name of the header being set
         * @param value the value of the header being set
         * @return this builder
         */
        public Self setHeader(CharSequence name, Object value) {
            ensureHeaders().set(name, value);
            return (Self) this;
        }

        /**
         * Sets a header with the specified name and value for this request.
         * 
         * @param name  the name of the header being set
         * @param value the value of the header being set
         * @return this builder
         */
        public Self setHeader(CharSequence name, int value) {
            ensureHeaders().setInt(name, value);
            return (Self) this;
        }

        /**
         * Sets a header with the specified name and values for this request.
         * 
         * @param name   the name of the header being set
         * @param values the values of the header being set
         * @return this builder
         */
        public Self setHeader(CharSequence name, Iterable<?> values) {
            ensureHeaders().set(name, values);
            return (Self) this;
        }

        /**
         * Sets the {@code content-type} header with the specified value for this
         * request.
         * 
         * @param value the value of the {@code content-type}
         * @return this builder
         */
        public Self contentType(CharSequence value) {
            return setHeader(CONTENT_TYPE, value);
        }

        /**
         * Sets the {@code content-type} header with the specified value for this
         * request.
         * 
         * @param contentType the value of the {@code content-type}
         * @param charset     the {@link Charset} of the {@code content-type}
         * @return this builder
         */
        public Self contentType(CharSequence contentType, Charset charset) {
            return contentType(HttpUtil.contentType(AsciiString.of(contentType), charset));
        }

        /**
         * Ensure the HTTP trailing headers for this request.
         * 
         * @return this builder
         */
        private HttpHeaders ensureTrailingHeaders() {
            if (trailingHeaders == null) {
                trailingHeaders = new DefaultHttpHeaders();
            }
            return trailingHeaders;
        }

        /**
         * Adds a new trailing header with the specified name and value for this
         * request.
         * 
         * @param name  the name of the trailing header being added
         * @param value the value of the trailing header being added
         * @return this builder
         */
        public Self trailing(CharSequence name, Object value) {
            ensureTrailingHeaders().add(name, value);
            return (Self) this;
        }

        /**
         * Adds a new trailing header with the specified name and value for this
         * request.
         * 
         * @param name  the name of the trailing header being added
         * @param value the value of the trailing header being added
         * @return this builder
         */
        public Self trailing(CharSequence name, int value) {
            ensureTrailingHeaders().addInt(name, value);
            return (Self) this;
        }

        /**
         * Adds a new trailing header with the specified name and values for this
         * request.
         * 
         * @param name   the name of the trailing header being added
         * @param values the values of the trailing header being added
         * @return this builder
         */
        public Self trailing(CharSequence name, Iterable<?> values) {
            ensureTrailingHeaders().add(name, values);
            return (Self) this;
        }

        /**
         * Sets a trailing header with the specified name and value for this request.
         * 
         * @param name  the name of the trailing header being set
         * @param value the value of the trailing header being set
         * @return this builder
         */
        public Self setTrailing(CharSequence name, Object value) {
            ensureTrailingHeaders().set(name, value);
            return (Self) this;
        }

        /**
         * Sets a trailing header with the specified name and value for this request.
         * 
         * @param name  the name of the trailing header being set
         * @param value the value of the trailing header being set
         * @return this builder
         */
        public Self setTrailing(CharSequence name, int value) {
            ensureTrailingHeaders().setInt(name, value);
            return (Self) this;
        }

        /**
         * Sets a trailing header with the specified name and values for this request.
         * 
         * @param name   the name of the trailing header being set
         * @param values the values of the trailing header being set
         * @return this builder
         */
        public Self setTrailing(CharSequence name, Iterable<?> values) {
            ensureTrailingHeaders().set(name, values);
            return (Self) this;
        }

        /**
         * Returns a new HTTP request built from the current state of this builder.
         * 
         * @return a {@link Request}
         */
        protected Request build() {
            Objects.requireNonNull(uri, "uri must not be null");
            ensureHeaders();
            ensureTrailingHeaders();
            return build0();
        }

        protected abstract Request build0();

    }

    /**
     * A HTTP request with a {@link HttpClient} wrapped.
     * 
     * @since 1.0
     * 
     * @author MJ Fang
     */
    interface ClientWrappedRequest extends Request {

        /**
         * Returns the wrapped HTTP client.
         * 
         * @return a {@link HttpClient}
         */
        HttpClient wrappedClient();

        /**
         * Sends this request asynchronously using the wrapped client with the given
         * response content handler.
         * 
         * <p>
         * <b>Note: The returned {@link CompletableFuture} will run on the netty
         * threads!</b>
         * 
         * @param <T>            the response content type
         * @param contentHandler the response content handler
         * @return a {@code CompletableFuture<HttpClient.Response<T>>}
         */
        default <T> CompletableFuture<Response<T>> sendAsync(HttpContentHandler<T> contentHandler) {
            return wrappedClient().sendAsync(this, contentHandler);
        }

        /**
         * Sends this request asynchronously using the wrapped client with the given
         * response content handler.
         * 
         * @param <T>            the response content type
         * @param contentHandler the response content handler
         * @param executor       the executor to use for asynchronous execution
         * @return a {@code CompletableFuture<HttpClient.Response<T>>}
         */
        default <T> CompletableFuture<Response<T>> sendAsync(HttpContentHandler<T> contentHandler, Executor executor) {
            return wrappedClient().sendAsync(this, contentHandler, executor);
        }

        /**
         * Sends this request using the wrapped client, blocking if necessary to get the
         * response.
         * 
         * @param <T>            the response content type
         * @param contentHandler the response content handler
         * @return the response
         * @throws IOException          if an I/O error occurs when sending or receiving
         * @throws InterruptedException if the operation is interrupted
         * @throws HttpRuntimeException if any other error occurs
         */
        default <T> Response<T> send(HttpContentHandler<T> contentHandler)
                throws IOException, InterruptedException, HttpRuntimeException {
            return wrappedClient().send(this, contentHandler);
        }

    }

    abstract class ClientWrappedRequestBuilder<Self extends ClientWrappedRequestBuilder<?>>
            extends RequestBuilder<Self> {

        protected final HttpClient wrappedClient;

        protected ClientWrappedRequestBuilder(HttpClient wrappedClient) {
            this.wrappedClient = wrappedClient;
        }

        @Override
        public ClientWrappedRequest get() {
            return (ClientWrappedRequest) super.get();
        }

        @Override
        public ClientWrappedRequest options() {
            return (ClientWrappedRequest) super.options();
        }

        @Override
        public ClientWrappedRequest head() {
            return (ClientWrappedRequest) super.head();
        }

        @Override
        public ClientWrappedRequest trace() {
            return (ClientWrappedRequest) super.trace();
        }

        @Override
        public ClientWrappedRequest post(HttpContentHolder<?> contentHolder) {
            return (ClientWrappedRequest) super.post(contentHolder);
        }

        @Override
        public ClientWrappedRequest put(HttpContentHolder<?> contentHolder) {
            return (ClientWrappedRequest) super.put(contentHolder);
        }

        @Override
        public ClientWrappedRequest patch(HttpContentHolder<?> contentHolder) {
            return (ClientWrappedRequest) super.patch(contentHolder);
        }

        @Override
        public ClientWrappedRequest delete(HttpContentHolder<?> contentHolder) {
            return (ClientWrappedRequest) super.delete(contentHolder);
        }

        @Override
        protected abstract ClientWrappedRequest build0();

        @Override
        protected ClientWrappedRequest build() {
            return (ClientWrappedRequest) super.build();
        }

    }

    /**
     * HTTP response.
     *
     * @param <T> Type of response content
     * 
     * @since 1.0
     * 
     * @author MJ Fang
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

    /**
     * A builder to build {@link HttpClient}.
     * 
     * @since 1.0
     *
     * @author MJ Fang
     */
    interface Builder {

        /**
         * Returns a new {@link HttpClient}.
         * 
         * @return a new {@code HttpClient}
         */
        HttpClient build();

        /**
         * Sets the timeout duration for this client.
         * 
         * @param duration the timeout {@link Duration}
         * @return this {@code Builder}
         */
        Builder timeout(Duration duration);

        /**
         * Sets the max content length for this client.
         * 
         * @param maxContentLength the max content length
         * @return this {@code Builder}
         */
        Builder maxContentLength(int maxContentLength);

        /**
         * Sets the SSL context for this client.
         * 
         * @param sslContext the {@link SslContext}
         * @return this {@code Builder}
         */
        Builder sslContext(SslContext sslContext);

        /**
         * Sets if the content compression feature is enabled or not.
         * 
         * @param enabled {@code true} if enabled
         * @return this {@code Builder}
         */
        Builder compression(boolean enabled);

    }

}
