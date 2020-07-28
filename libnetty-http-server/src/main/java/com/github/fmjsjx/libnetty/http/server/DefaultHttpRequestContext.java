package com.github.fmjsjx.libnetty.http.server;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Optional;

import com.github.fmjsjx.libnetty.http.HttpUtil;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
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

    private String remoteAddress;
    private QueryStringDecoder queryStringDecoder;

    private final LinkedHashMap<Object, Object> properties = new LinkedHashMap<>();

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
    }

    @Override
    public long recievedNanoTime() {
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
            remoteAddress = addr = HttpUtil.remoteAddress(channel(), headers());
        }
        return addr;
    }

    @Override
    public FullHttpRequest request() {
        return request;
    }

    @Override
    public QueryStringDecoder queryStringDecoder() {
        QueryStringDecoder decoder = queryStringDecoder;
        if (decoder == null) {
            queryStringDecoder = decoder = new QueryStringDecoder(request().uri());
        }
        return decoder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> property(Object key) {
        Object value = properties.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    @Override
    public <T> Optional<T> property(Object key, Class<T> type) {
        Object value = properties.get(key);
        return Optional.ofNullable(value).map(type::cast);
    }

    @Override
    public DefaultHttpRequestContext property(Object key, Object value) {
        properties.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder().append("DefaultHttpRequestContext(receivedTime: ").append(receivedTime)
                .append(", channel: ").append(channel()).append(", remoteAddress: ").append(remoteAddress())
                .append(", query: ").append(queryStringDecoder).append(")\n");
        b.append(request().toString());
        return b.toString();
    }

}
