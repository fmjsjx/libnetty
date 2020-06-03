package com.github.fmjsjx.libnetty.http.client;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Implementations of {@link HttpContentHolder}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpContentHolders {

    /**
     * Returns a new {@link HttpContentHolder} with the specified value in
     * {@code ISO_8859_1}.
     * 
     * @param value a {@code String} value
     * @return a {@code HttpContentHolder<String>}
     */
    public static final HttpContentHolder<String> ofLatin1(String value) {
        return ofString(value, CharsetUtil.ISO_8859_1);
    }

    /**
     * Returns a new {@link HttpContentHolder} with the specified value in
     * {@code US_ASCII}.
     * 
     * @param value a {@code String} value
     * @return a {@code HttpContentHolder<String>}
     */
    public static final HttpContentHolder<String> ofAscii(String value) {
        return ofString(value, CharsetUtil.US_ASCII);
    }

    /**
     * Returns a new {@link HttpContentHolder} with the specified value in
     * {@code UTF-8}.
     * 
     * @param value a {@code String} value
     * @return a {@code HttpContentHolder<String>}
     */
    public static final HttpContentHolder<String> ofUtf8(String value) {
        return ofString(value, CharsetUtil.UTF_8);
    }

    /**
     * Returns a new {@link HttpContentHolder} with the specified value.
     * 
     * @param value   a {@code String} value
     * @param charset the {@code Charset} of the value
     * @return a {@code HttpContentHolder<String>}
     */
    public static final HttpContentHolder<String> ofString(String value, Charset charset) {
        return new StringHolder(value, charset);
    }

    /**
     * Returns a new {@link HttpContentHolder} with the specified byte array value.
     * 
     * @param value the {@code byte[]} value
     * @return a {@code HttpContentHolder<byte[]>}
     */
    public static final HttpContentHolder<byte[]> ofByteArray(byte[] value) {
        return new ByteArrayHolder(value);
    }

    /**
     * Returns a singleton {@link HttpContentHolder} instance with empty value.
     * 
     * @return a singleton {@code HttpContentHolder<ByteBuf>} with content
     *         {@link Unpooled#EMPTY_BUFFER}
     */
    public static final HttpContentHolder<?> ofEmpty() {
        return EmptyHolder.instance;
    }

    private static final class StringHolder extends HttpContentHolder<String> {

        private final Charset charset;

        private StringHolder(String base, Charset charset) {
            super(base);
            this.charset = charset;
        }

        @Override
        protected ByteBuf encode(ByteBufAllocator alloc, String base) {
            if (charset.equals(CharsetUtil.UTF_8)) {
                return ByteBufUtil.writeUtf8(alloc, base);
            } else if (charset.equals(CharsetUtil.US_ASCII) || charset.equals(CharsetUtil.ISO_8859_1)) {
                return ByteBufUtil.writeAscii(alloc, base);
            } else {
                byte[] b = base.getBytes(charset);
                ByteBuf content = alloc.buffer(b.length);
                content.writeBytes(b);
                return content;
            }
        }

    }

    private static final class ByteArrayHolder extends HttpContentHolder<byte[]> {

        private ByteArrayHolder(byte[] base) {
            super(base);
        }

        @Override
        protected ByteBuf encode(ByteBufAllocator alloc, byte[] base) {
            ByteBuf content = alloc.buffer(base.length);
            content.writeBytes(base);
            return content;
        }

    }

    private static final class EmptyHolder extends HttpContentHolder<ByteBuf> {

        private static final EmptyHolder instance = new EmptyHolder();

        private EmptyHolder() {
            super(Unpooled.EMPTY_BUFFER);
        }

        @Override
        protected ByteBuf encode(ByteBufAllocator alloc, ByteBuf base) {
            throw new UnsupportedOperationException();
        }

        @Override
        ByteBuf content(ByteBufAllocator alloc) {
            return base();
        }

    }

}
