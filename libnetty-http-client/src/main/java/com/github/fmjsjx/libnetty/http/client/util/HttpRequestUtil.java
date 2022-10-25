package com.github.fmjsjx.libnetty.http.client.util;

import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.util.Arrays;
import java.util.Base64;

/**
 * The utility class of HTTP request.
 *
 * @author MJ Fang
 *
 * @since 2.6
 */
public class HttpRequestUtil {

    private static final class BasicBytesHolder {
        private static final byte[] instance = "Basic ".getBytes();

        private static final byte[] newBasicValue(int base64Length) {
            return Arrays.copyOf(instance, base64Length + 6);
        }

    }

    /**
     * Returns a new {@link AsciiString} follow the "HTTP Basic Authentication" protocol by the specified username
     * and the specified password given.
     * <p>Using the specified Base64 encoder.</p>
     *
     * @param username the username
     * @param password the password
     * @param encoder the Base64 encoder
     * @return a new {@link AsciiString} follow the "HTTP Basic Authentication" protocol
     */
    public static final AsciiString authBasic(String username, String password, Base64.Encoder encoder) {
        var builder = new StringBuilder();
        if (username != null) {
            builder.append(username);
        }
        if (password != null) {
            builder.append(":").append(password);
        }

        var bytes = encoder.encode(builder.toString().getBytes(CharsetUtil.UTF_8));
        var value = BasicBytesHolder.newBasicValue(bytes.length);
        System.arraycopy(bytes, 0, value, 6, bytes.length);
        return new AsciiString(value, false);
    }

    private HttpRequestUtil() {}

}
