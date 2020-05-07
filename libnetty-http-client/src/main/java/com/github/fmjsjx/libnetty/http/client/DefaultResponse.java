package com.github.fmjsjx.libnetty.http.client;

import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class DefaultResponse<T> implements Response<T> {

    private final HttpVersion version;
    private final HttpResponseStatus status;
    private final HttpHeaders headers;
    private final T content;

    @Override
    public HttpVersion version() {
        return version;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public T content() {
        return content;
    }

}
