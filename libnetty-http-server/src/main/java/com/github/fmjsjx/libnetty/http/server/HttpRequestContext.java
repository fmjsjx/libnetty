package com.github.fmjsjx.libnetty.http.server;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;

public interface HttpRequestContext {

    long recievedNanoTime();

    ZonedDateTime receivedTime();

    default ZonedDateTime receivedTime(ZoneId zone) {
        return receivedTime().withZoneSameLocal(zone);
    }

    default LocalDateTime receivedLocalTime() {
        return receivedTime().toLocalDateTime();
    }

    Channel channel();

    default EventLoop eventLoop() {
        return channel().eventLoop();
    }

    String remoteAddress();

    FullHttpRequest request();

    default HttpHeaders headers() {
        return request().headers();
    }

    default HttpHeaders trailingHeaders() {
        return request().trailingHeaders();
    }

    QueryStringDecoder queryStringDecoder();

    default String rawPath() {
        return queryStringDecoder().rawPath();
    }

    default String rawQuery() {
        return queryStringDecoder().rawQuery();
    }

    default Map<String, List<String>> queryParameters() {
        return queryStringDecoder().parameters();
    }

}
