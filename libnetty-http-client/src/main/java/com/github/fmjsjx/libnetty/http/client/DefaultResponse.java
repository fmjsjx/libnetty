package com.github.fmjsjx.libnetty.http.client;

import com.github.fmjsjx.libnetty.http.client.HttpClient.Response;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.ToString;

import java.util.function.Supplier;

@ToString
class DefaultResponse<T> implements Response<T> {

    private final HttpVersion version;
    private final HttpResponseStatus status;
    private final HttpHeaders headers;
    private final T content;
    private final Supplier<HttpHeaders> trailingHeadersSupplier;

    DefaultResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers, T content,
                    Supplier<HttpHeaders> trailingHeadersSupplier) {
        this.version = version;
        this.status = status;
        this.headers = headers;
        this.content = content;
        this.trailingHeadersSupplier = trailingHeadersSupplier;
    }

    DefaultResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers, T content,
                    HttpHeaders trailingHeaders) {
        this(version, status, headers, content, () -> trailingHeaders);
    }

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

    @Override
    public HttpHeaders trailingHeaders() {
        var trailingHeaders = trailingHeadersSupplier.get();
        return trailingHeaders != null ? trailingHeaders : EmptyHttpHeaders.INSTANCE;
    }

}
