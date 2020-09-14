package com.github.fmjsjx.libnetty.http.server;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * The default implementation of {@link DefaultHttpRequestContext}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
class DefaultHttpRequestContext implements HttpRequestContext {

    private final long recievedNanoTime = System.nanoTime();
    private final ZonedDateTime receivedTime = ZonedDateTime.now();

    private final Channel channel;
    private final FullHttpRequest request;
    private final int contentLength;

    private String remoteAddress;
    private Optional<CharSequence> contentType;
    private QueryStringDecoder queryStringDecoder;

    private final ConcurrentMap<Object, Object> properties = new ConcurrentHashMap<>();

    /**
     * Creates a new {@link DefaultHttpRequestContext} with the specified
     * {@link Channel} and {@link FullHttpRequest}.
     * 
     * @param channel the channel
     * @param request the HTTP request
     */
    DefaultHttpRequestContext(Channel channel, FullHttpRequest request) {
        this.channel = channel;
        this.request = request;
        this.contentLength = request.content().readableBytes();
    }

    @Override
    public long receivedNanoTime() {
        return recievedNanoTime;
    }

    @Override
    public ZonedDateTime receivedTime() {
        return receivedTime;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String remoteAddress() {
        String addr = remoteAddress;
        if (addr == null) {
            remoteAddress = addr = com.github.fmjsjx.libnetty.http.HttpUtil.remoteAddress(channel(), headers());
        }
        return addr;
    }

    @Override
    public FullHttpRequest request() {
        return request;
    }

    @Override
    public int contentLength() {
        return contentLength;
    }

    @Override
    public Optional<CharSequence> contentType() {
        Optional<CharSequence> contentType = this.contentType;
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
        return properties.get(key);
    }

    @Override
    public <T> Optional<T> property(Object key, Class<T> type) {
        Object value = getProperty(key);
        return Optional.ofNullable(value).map(type::cast);
    }

    @Override
    public DefaultHttpRequestContext property(Object key, Object value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
        return this;
    }

    @Override
    public boolean hasProperty(Object key) {
        return properties.containsKey(key);
    }

    @Override
    public Stream<Object> propertyKeys() {
        return properties.keySet().stream();
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

}
