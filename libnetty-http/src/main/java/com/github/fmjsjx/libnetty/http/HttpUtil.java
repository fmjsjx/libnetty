package com.github.fmjsjx.libnetty.http;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtil {

    public static final String remoteAddress(Channel channel, HttpHeaders headers) {
        String address = headers.get(HttpHeaderXNames.X_FORWARDED_FOR);
        if (address == null) {
            return ((InetSocketAddress) channel.remoteAddress()).getHostString();
        }
        return address;
    }

    public static final AsciiString contentType(AsciiString contentType) {
        // Make UTF-8 as default Character Set.
        return contentType(contentType, CharsetUtil.UTF_8);
    }

    public static final AsciiString contentType(AsciiString contentType, Charset charset) {
        Objects.requireNonNull(charset, "charset must not be null");
        return CachedContentTypeHolder.cachedContentTypes.computeIfAbsent(contentType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(charset, k -> AsciiString.cached(contentType.toString() + ";charset=" + k.name()));
    }

    private static final class CachedContentTypeHolder {
        private static final ConcurrentMap<AsciiString, ConcurrentMap<Charset, AsciiString>> cachedContentTypes = new ConcurrentHashMap<>();
    }

}
