package com.github.fmjsjx.libnetty.http;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * Common utility class for HTTP.
 * 
 * @since 1.2
 *
 * @author MJ Fang
 */
public final class HttpCommonUtil {

    /**
     * Returns remote address from specified {@code channel} and {@code headers} on
     * server side.
     * 
     * @param channel the channel
     * @param headers the headers
     * @return the remote address of the client
     */
    public static final String remoteAddress(Channel channel, HttpHeaders headers) {
        String address = headers.get(HttpHeaderXNames.X_FORWARDED_FOR);
        if (address == null) {
            return ((InetSocketAddress) channel.remoteAddress()).getHostString();
        }
        // Value of header "x-forwarded-for" may have more than one address.
        // Always take the first address.
        // See: https://github.com/fmjsjx/libnetty/issues/74
        var index = address.indexOf(",");
        if (index > 0) {
            return address.substring(0, index);
        }
        return address;
    }

    /**
     * Returns the {@code content-type} header value with {@code UTF-8} character
     * set parameter.
     * 
     * @param contentType the content type
     * @return a {@code AsciiString}
     */
    public static final AsciiString contentType(AsciiString contentType) {
        // Make UTF-8 as default Character Set.
        return contentType(contentType, CharsetUtil.UTF_8);
    }

    /**
     * Returns the {@code content-type} header value with the specified
     * {@link Charset}.
     * 
     * @param contentType the content type
     * @param charset     the {@code Charset}
     * @return a {@code AsciiString}
     */
    public static final AsciiString contentType(AsciiString contentType, Charset charset) {
        Objects.requireNonNull(charset, "charset must not be null");
        return CachedContentTypeHolder.cachedContentTypes.computeIfAbsent(contentType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(charset, k -> AsciiString.cached(contentType.toString() + "; charset=" + k.name()));
    }

    private static final class CachedContentTypeHolder {
        private static final ConcurrentMap<AsciiString, ConcurrentMap<Charset, AsciiString>> cachedContentTypes = new ConcurrentHashMap<>();
    }

    private static final byte[] BASIC_ = "Basic ".getBytes();

    /**
     * Returns the encoded basic authentication string.
     * 
     * @param user     the user name
     * @param password the password
     * @return the encoded basic authentication
     */
    public static final CharSequence basicAuthentication(String user, String password) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(256)) {
            out.write(BASIC_);
            try (OutputStream bout = Base64.getEncoder().wrap(out)) {
                bout.write((user + ":" + password).getBytes(CharsetUtil.UTF_8));
            }
            return new AsciiString(out.toByteArray(), false);
        } catch (Exception e) {
            // can't reach this line
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns {@code true} if the status code is success, {@code false} otherwise.
     * 
     * @param statusCode the status code
     * @return {@code true} if the status code is success, {@code false} otherwise
     */
    public static final boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Returns {@code true} if the status is success, {@code false} otherwise.
     * 
     * @param status the status
     * @return {@code true} if the status is success, {@code false} otherwise
     */
    public static final boolean isSuccess(HttpResponseStatus status) {
        return isSuccessStatus(status.code());
    }

    /**
     * Returns {@code true} if the status is success, {@code false} otherwise.
     * 
     * @param response the response
     * @return {@code true} if the status is success, {@code false} otherwise
     */
    public static final boolean isSuccess(HttpResponse response) {
        return isSuccess(response.status());
    }

    private HttpCommonUtil() {
    }

}
