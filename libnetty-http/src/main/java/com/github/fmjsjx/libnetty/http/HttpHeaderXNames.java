package com.github.fmjsjx.libnetty.http;

import io.netty.util.AsciiString;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpHeaderXNames {

    /**
     * {@code "x-forwarded-for"}
     */
    public static final AsciiString X_FORWARDED_FOR = AsciiString.cached("x-forwarded-for");
    /**
     * {@code "x-forwarded-host"}
     */
    public static final AsciiString X_FORWARDED_HOST = AsciiString.cached("x-forwarded-host");
    /**
     * {@code "x-forwarded-proto"}
     */
    public static final AsciiString X_FORWARDED_PROTO = AsciiString.cached("x-forwarded-proto");
    /**
     * {@code "x-real-ip"}
     */
    public static final AsciiString X_REAL_IP = AsciiString.cached("x-real-ip");

}
