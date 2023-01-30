package com.github.fmjsjx.libnetty.http.client;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import com.github.fmjsjx.libnetty.http.client.HttpClient.ClientWrappedRequest;
import com.github.fmjsjx.libnetty.http.client.HttpClient.ClientWrappedRequestBuilder;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class DefaultRequest implements ClientWrappedRequest {

    private final HttpClient wrappedClient;

    private final HttpMethod method;
    private final URI uri;
    private final HttpHeaders headers;
    private final HttpHeaders trailingHeaders;
    private final HttpContentHolder<?> contentHolder;
    private final Optional<Duration> timeout;
    private final Optional<MultipartBody> multipartBody;

    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return trailingHeaders;
    }

    @Override
    public HttpContentHolder<?> contentHolder() {
        return contentHolder;
    }

    @Override
    public HttpClient wrappedClient() {
        return wrappedClient;
    }

    @Override
    public Optional<Duration> timeout() {
        return timeout;
    }

    @Override
    public Optional<MultipartBody> multipartBody() {
        return multipartBody;
    }

    static final class Builder extends ClientWrappedRequestBuilder<Builder> {

        Builder(HttpClient wrappedClient) {
            super(wrappedClient);
        }

        Builder() {
            this(null);
        }

        @Override
        protected ClientWrappedRequest build0() {
            return new DefaultRequest(wrappedClient, method, uri, headers, trailingHeaders, contentHolder,
                    Optional.ofNullable(timeout), Optional.ofNullable(multipartBody));
        }

    }

}