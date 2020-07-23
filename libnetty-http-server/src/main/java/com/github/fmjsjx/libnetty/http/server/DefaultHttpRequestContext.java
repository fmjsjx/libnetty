package com.github.fmjsjx.libnetty.http.server;

import java.time.ZonedDateTime;

import com.github.fmjsjx.libnetty.http.HttpUtil;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public class DefaultHttpRequestContext implements HttpRequestContext {

    private final long recievedNanoTime = System.nanoTime();
    private final ZonedDateTime receivedTime = ZonedDateTime.now();

    private final Channel channel;
    private final FullHttpRequest request;

    private String remoteAddress;
    private QueryStringDecoder queryStringDecoder;

    public DefaultHttpRequestContext(Channel channel, FullHttpRequest request) {
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

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder().append("DefaultHttpRequestContext(receivedTime: ").append(receivedTime)
                .append(", channel: ").append(channel()).append(", remoteAddress: ").append(remoteAddress())
                .append(", query: ").append(queryStringDecoder).append(")\n");
        b.append(request().toString());
        return b.toString();
    }

}
