package com.github.fmjsjx.libnetty.http.server;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.fmjsjx.libnetty.http.HttpCommonUtil;
import com.github.fmjsjx.libnetty.http.server.component.HttpServerComponent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.Http2StreamChannel;

/**
 * The default implementation of {@link DefaultHttpRequestContext}.
 *
 * @author MJ Fang
 * @since 1.1
 */
class DefaultHttpRequestContext implements HttpRequestContext {

    private static final Function<Object, String> PROPERTY_KEY_ENCODER = String::valueOf;

    private final long receivedNanoTime = System.nanoTime();
    private final ZonedDateTime receivedTime = ZonedDateTime.now();
    private LocalDateTime receivedLocalTime;

    private final Channel channel;
    private final FullHttpRequest request;
    private final int contentLength;

    private String remoteAddress;
    private int keepAliveFlag = -1;
    private Optional<CharSequence> contentType;
    private QueryStringDecoder queryStringDecoder;
    private String rawPath;
    private String rawQuery;
    private final AtomicReference<PathVariables> pathVariablesRef = new AtomicReference<>();

    private final Map<Class<?>, Object> components;
    private final ConcurrentMap<Object, Object> properties = new ConcurrentHashMap<>();
    private final HttpResponseFactoryImpl responseFactory = new HttpResponseFactoryImpl();
    private final Optional<Consumer<HttpHeaders>> addHeaders;
    private final boolean sslEnabled;
    private final String protocolVersion;

    DefaultHttpRequestContext(Channel channel, FullHttpRequest request, Map<Class<?>, Object> components,
                              Consumer<HttpHeaders> addHeaders, boolean sslEnabled) {
        this.channel = channel;
        this.request = request;
        this.contentLength = request.content().readableBytes();
        this.components = components;
        this.addHeaders = Optional.ofNullable(addHeaders);
        this.sslEnabled = sslEnabled;
        if (channel instanceof Http2StreamChannel) {
            protocolVersion = sslEnabled ? "h2" : "h2c";
        } else {
            protocolVersion = version().text();
        }
    }

    @Override
    public long receivedNanoTime() {
        return receivedNanoTime;
    }

    @Override
    public ZonedDateTime receivedTime() {
        return receivedTime;
    }

    @Override
    public LocalDateTime receivedLocalTime() {
        var receivedLocalTime = this.receivedLocalTime;
        if (receivedLocalTime == null) {
            this.receivedLocalTime = receivedLocalTime = receivedTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
        return receivedLocalTime;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String remoteAddress() {
        String addr = remoteAddress;
        if (addr == null) {
            remoteAddress = addr = HttpCommonUtil.remoteAddress(channel(), headers());
        }
        return addr;
    }

    @Override
    public FullHttpRequest request() {
        return request;
    }

    @Override
    public boolean isKeepAlive() {
        int flag = keepAliveFlag;
        if (flag == -1) {
            keepAliveFlag = flag = HttpUtil.isKeepAlive(request()) ? 1 : 0;
        }
        return flag == 1;
    }

    @Override
    public String protocolVersion() {
        return protocolVersion;
    }

    @Override
    public boolean sslEnabled() {
        return sslEnabled;
    }

    @Override
    public int contentLength() {
        return contentLength;
    }

    @Override
    public Optional<CharSequence> contentType() {
        Optional<CharSequence> contentType = this.contentType;
        //noinspection OptionalAssignedToNull
        if (contentType == null) {
            this.contentType = contentType = Optional.ofNullable(HttpUtil.getMimeType(request));
        }
        return contentType;
    }

    @Override
    public QueryStringDecoder queryStringDecoder() {
        QueryStringDecoder decoder = queryStringDecoder;
        if (decoder == null) {
            queryStringDecoder = decoder = new QueryStringDecoder(request().uri());
        }
        return decoder;
    }

    @Override
    public String rawPath() {
        var rawPath = this.rawPath;
        if (rawPath == null) {
            this.rawPath = rawPath = queryStringDecoder().rawPath();
        }
        return rawPath;
    }

    @Override
    public String rawQuery() {
        var rawQuery = this.rawQuery;
        if (rawQuery == null) {
            this.rawQuery = rawQuery = queryStringDecoder().rawQuery();
        }
        return rawQuery;
    }

    @Override
    public PathVariables pathVariables() {
        return pathVariablesRef.get();
    }

    @Override
    public DefaultHttpRequestContext pathVariables(PathVariables pathVariables) {
        this.pathVariablesRef.set(pathVariables);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends HttpServerComponent> Optional<C> component(Class<? extends C> componentType) {
        return (Optional<C>) components.getOrDefault(componentType, Optional.empty());
    }

    @Override
    public <T> Optional<T> property(Object key) {
        Object value = getProperty(key);
        if (value == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        T t = (T) value;
        return Optional.of(t);
    }

    private Object getProperty(Object key) {
        return properties.get(PROPERTY_KEY_ENCODER.apply(key));
    }

    @Override
    public <T> Optional<T> property(Object key, Class<T> type) {
        Object value = getProperty(key);
        return Optional.ofNullable(value).map(type::cast);
    }

    @Override
    public DefaultHttpRequestContext property(Object key, Object value) {
        String keyName = PROPERTY_KEY_ENCODER.apply(key);
        if (value == null) {
            properties.remove(keyName);
        } else {
            properties.put(keyName, value);
        }
        return this;
    }

    @Override
    public boolean hasProperty(Object key) {
        return properties.containsKey(PROPERTY_KEY_ENCODER.apply(key));
    }

    @Override
    public Stream<String> propertyKeyNames() {
        return properties.keySet().stream().map(PROPERTY_KEY_ENCODER);
    }

    @Override
    public HttpResponseFactory responseFactory() {
        return responseFactory;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder().append("DefaultHttpRequestContext(receivedTime: ").append(receivedTime)
                .append(", channel: ").append(channel()).append(", remoteAddress: ").append(remoteAddress())
                .append(", query: ").append(queryStringDecoder).append(", contentLength: ").append(contentLength)
                .append(", properties: ").append(properties).append(")\n");
        b.append(request().toString());
        return b.toString();
    }

    private class HttpResponseFactoryImpl implements HttpResponseFactory {

        @Override
        public HttpResponse create(HttpResponseStatus status) {
            DefaultHttpResponse response = new DefaultHttpResponse(version(), status);
            initHeaders(response);
            return response;
        }

        private HttpHeaders initHeaders(HttpResponse response) {
            HttpHeaders headers = response.headers();
            Optional<Consumer<HttpHeaders>> addHeaders = DefaultHttpRequestContext.this.addHeaders;
            addHeaders.ifPresent(action -> action.accept(headers));
            HttpUtil.setKeepAlive(headers, response.protocolVersion(), isKeepAlive());
            return headers;
        }

        @Override
        public FullHttpResponse createFull(HttpResponseStatus status) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(version(), status, Unpooled.EMPTY_BUFFER);
            HttpHeaders headers = initHeaders(response);
            headers.set(CONTENT_LENGTH, ZERO);
            return response;
        }

        @Override
        public FullHttpResponse createFull(HttpResponseStatus status, ByteBuf content, int contentLength,
                                           CharSequence contentType) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(version(), status, content);
            HttpHeaders headers = initHeaders(response);
            headers.setInt(CONTENT_LENGTH, contentLength);
            headers.set(CONTENT_TYPE, contentType);
            return response;
        }

        @Override
        public FullHttpResponse createFullText(HttpResponseStatus status) {
            byte[] b = status.toString().getBytes();
            ByteBuf content = alloc().buffer(b.length, b.length).writeBytes(b);
            return createFull(status, content, b.length, TEXT_PLAIN_UTF8);
        }

        @Override
        public FullHttpResponse createFullText(HttpResponseStatus status, Charset charset) {
            byte[] b = status.toString().getBytes();
            ByteBuf content = alloc().buffer(b.length, b.length).writeBytes(b);
            CharSequence contentType = HttpCommonUtil.contentType(TEXT_PLAIN, charset);
            return createFull(status, content, b.length, contentType);
        }

    }

}
