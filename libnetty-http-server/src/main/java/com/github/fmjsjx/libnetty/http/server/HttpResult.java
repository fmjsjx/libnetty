package com.github.fmjsjx.libnetty.http.server;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface HttpResult {

    HttpRequestContext requestContext();

    long resultLength();

    HttpResponseStatus responseStatus();

    long respondedNaonTime();

    ZonedDateTime respondedTime();

    default ZonedDateTime respondedTime(ZoneId zone) {
        return respondedTime().withZoneSameLocal(zone);
    }

    default long timeUsed(TimeUnit unit) {
        return unit.convert(respondedNaonTime() - requestContext().recievedNanoTime(), TimeUnit.NANOSECONDS);
    }

}
