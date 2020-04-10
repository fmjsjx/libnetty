package com.github.fmjsjx.libnetty.http;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtil {

    public static final String remoteAddress(Channel channel, HttpHeaders headers) {
        String address = headers.get(HttpXheaderNames.X_FORWARDED_FOR);
        if (address == null) {
            return ((InetSocketAddress) channel.remoteAddress()).getHostString();
        }
        return address;
    }

}
