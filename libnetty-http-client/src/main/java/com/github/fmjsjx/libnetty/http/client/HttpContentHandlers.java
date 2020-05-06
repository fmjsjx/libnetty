package com.github.fmjsjx.libnetty.http.client;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Implementations of {@link HttpContentHandler}.
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpContentHandlers {

    private static final class ByteArrayHandlerHolder {
        private static final HttpContentHandler<byte[]> BYTE_ARRAY_HANDLER = ByteBufUtil::getBytes;
    }

    /**
     * Returns a {@link HttpContentHandler} which stores the HTTP content as a
     * {@code byte[]}.
     * 
     * @return a {@link HttpContentHandler}
     */
    public static final HttpContentHandler<byte[]> ofByteArray() {
        return ByteArrayHandlerHolder.BYTE_ARRAY_HANDLER;
    }

    private static final class StringHandlerHolder {
        private static final ConcurrentMap<Charset, HttpContentHandler<String>> STRING_HANDLERS = new ConcurrentHashMap<>();
    }

    /**
     * Returns a {@link HttpContentHandler} which stores the HTTP content as a
     * {@link String} converted using the default character set {@code UTF-8}.
     * 
     * @return a {@link HttpContentHandler}
     */
    public static final HttpContentHandler<String> ofString() {
        return ofString(CharsetUtil.UTF_8);
    }

    /**
     * Returns a {@link HttpContentHandler} which stores the HTTP content as a
     * {@link String} converted using the given {@code charset}.
     * 
     * @param charset the character set to convert the String with
     * @return a {@link HttpContentHandler}
     */
    public static final HttpContentHandler<String> ofString(Charset charset) {
        return StringHandlerHolder.STRING_HANDLERS.computeIfAbsent(charset, k -> buf -> buf.toString(k));
    }

}
