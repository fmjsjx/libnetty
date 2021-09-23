package com.github.fmjsjx.libnetty.http;

import java.util.function.Consumer;

import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;

/**
 * A factory to create {@link HttpContentCompressor}s.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 * 
 * @see HttpContentCompressor
 */
public class HttpContentCompressorFactory {

    private static final int DEFAULT_COMPRESSION_LEVEL = 6;
    private static final int DEFAULT_WINDOW_BITS = 15;
    private static final int DEFAULT_MEM_LEVEL = 8;
    private static final int DEFAULT_CONTENT_SIZE_THRESHOLD = 1024;

    /**
     * Returns an apply action that just create builder with default settings but do
     * nothing.
     * 
     * @return a {@code Consumer<HttpContentCompressorFactory.Builder>}
     */
    public static final Consumer<Builder> defaultSettings() {
        return builder -> {
            // just create builder with default settings
        };
    }

    /**
     * Creates a new {@link Builder}.
     * 
     * @return a {@code HttpContentCompressorFactory.Builder}
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * A builder to build {@link HttpContentCompressorFactory}s.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     * @see HttpContentCompressorFactory
     */
    public static final class Builder {

        private int compressionLevel = DEFAULT_COMPRESSION_LEVEL;
        private int windowBits = DEFAULT_WINDOW_BITS;
        private int memLevel = DEFAULT_MEM_LEVEL;
        private int contentSizeThreshold = DEFAULT_CONTENT_SIZE_THRESHOLD;

        /**
         * Set the compression level. {@code 1} yields the fastest compression and
         * {@code 9} yields the best compression. {@code 0} means no compression. The
         * default compression level is {@code 6}.
         * 
         * @param compressionLevel the compression level
         * @return this builder
         */
        public Builder compressionLevel(int compressionLevel) {
            this.compressionLevel = compressionLevel;
            return this;
        }

        /**
         * Set the window bits, the base two logarithm of the size of the history
         * buffer. The value should be in the range {@code 9} to {@code 15} inclusive.
         * Larger values result in better compression at the expense of memory usage.
         * The default value is {@code 15}
         * 
         * @param windowBits the window bits
         * @return this builder
         */
        public Builder windowBits(int windowBits) {
            this.windowBits = windowBits;
            return this;
        }

        /**
         * Set the memory level, how much memory should be allocated for the internal
         * compression state. {@code 1} uses minimum memory and {@code 9} uses maximum
         * memory. Larger values result in better and faster compression at the expense
         * of memory usage. The default value is {@code 8}.
         * 
         * @param memLevel the memory level
         * @return this builder
         */
        public Builder memLevel(int memLevel) {
            this.memLevel = memLevel;
            return this;
        }

        /**
         * Set the content size threshold. The response body is compressed when the size
         * of the response body exceeds the threshold. The value should be a non
         * negative number. {@code 0} will enable compression for all responses. The
         * default value is {@code 1024}.
         * 
         * @param contentSizeThreshold the content size threshold
         * @return this builder
         */
        public Builder contentSizeThreshold(int contentSizeThreshold) {
            this.contentSizeThreshold = contentSizeThreshold;
            return this;
        }

        /**
         * Creates a new {@link HttpContentCompressorFactory} with the current settings.
         * 
         * @return a {@code HttpContentCompressorFactory}
         */
        public HttpContentCompressorFactory build() {
            return new HttpContentCompressorFactory(compressionLevel, windowBits, memLevel, contentSizeThreshold);
        }

        private Builder() {
        }

    }

    private final int compressionLevel;
    private final int windowBits;
    private final int memLevel;
    private final int contentSizeThreshold;

    /**
     * Creates a new factory with the default compression level ({@code 6}), default
     * window size ({@code 15}), default memory level ({@code 8}) and default
     * content size threshold ({@code 1024}).
     */
    public HttpContentCompressorFactory() {
        this(DEFAULT_COMPRESSION_LEVEL);
    }

    /**
     * Creates a new factory with the specified compression level, default window
     * size ({@code 15}), default memory level ({@code 8}) and default content size
     * threshold ({@code 1024}).
     * 
     * @param compressionLevel {@code 1} yields the fastest compression and
     *                         {@code 9} yields the best compression. {@code 0}
     *                         means no compression. The default compression level
     *                         is {@code 6}
     */
    public HttpContentCompressorFactory(int compressionLevel) {
        this(compressionLevel, DEFAULT_WINDOW_BITS, DEFAULT_MEM_LEVEL, DEFAULT_CONTENT_SIZE_THRESHOLD);
    }

    /**
     * Creates a new factory with the specified compression level, window size,
     * memory level and default content size threshold ({@code 1024}).
     * 
     * @param compressionLevel {@code 1} yields the fastest compression and
     *                         {@code 9} yields the best compression. {@code 0}
     *                         means no compression. The default compression level
     *                         is {@code 6}
     * @param windowBits       The base two logarithm of the size of the history
     *                         buffer. The value should be in the range {@code 9} to
     *                         {@code 15} inclusive. Larger values result in better
     *                         compression at the expense of memory usage. The
     *                         default value is {@code 15}
     * @param memLevel         How much memory should be allocated for the internal
     *                         compression state. {@code 1} uses minimum memory and
     *                         {@code 9} uses maximum memory. Larger values result
     *                         in better and faster compression at the expense of
     *                         memory usage. The default value is {@code 8}
     */
    public HttpContentCompressorFactory(int compressionLevel, int windowBits, int memLevel) {
        this(compressionLevel, windowBits, memLevel, DEFAULT_CONTENT_SIZE_THRESHOLD);
    }

    /**
     * Creates a new factory with the specified compression level, window size,
     * memory level and content size threshold.
     *
     * @param compressionLevel     {@code 1} yields the fastest compression and
     *                             {@code 9} yields the best compression. {@code 0}
     *                             means no compression. The default compression
     *                             level is {@code 6}
     * @param windowBits           The base two logarithm of the size of the history
     *                             buffer. The value should be in the range
     *                             {@code 9} to {@code 15} inclusive. Larger values
     *                             result in better compression at the expense of
     *                             memory usage. The default value is {@code 15}
     * @param memLevel             How much memory should be allocated for the
     *                             internal compression state. {@code 1} uses
     *                             minimum memory and {@code 9} uses maximum memory.
     *                             Larger values result in better and faster
     *                             compression at the expense of memory usage. The
     *                             default value is {@code 8}
     * @param contentSizeThreshold The response body is compressed when the size of
     *                             the response body exceeds the threshold. The
     *                             value should be a non negative number. {@code 0}
     *                             will enable compression for all responses. The
     *                             default value is {@code 1024}
     */
    public HttpContentCompressorFactory(int compressionLevel, int windowBits, int memLevel, int contentSizeThreshold) {
        this.compressionLevel = compressionLevel;
        this.windowBits = windowBits;
        this.memLevel = memLevel;
        this.contentSizeThreshold = contentSizeThreshold;
        // Try to create a new HttpContentCompressor for fields validation
        create();
    }

    /**
     * Returns the compression level.
     * 
     * @return the compression level
     */
    public int compressionLevel() {
        return compressionLevel;
    }

    /**
     * Returns the window size.
     * 
     * @return the window size
     */
    public int windowBits() {
        return windowBits;
    }

    /**
     * Returns the memory level.
     * 
     * @return the memory level
     */
    public int memLevel() {
        return memLevel;
    }

    /**
     * Returns the content size threshold.
     * 
     * @return the content size threshold
     */
    public int contentSizeThreshold() {
        return contentSizeThreshold;
    }

    /**
     * Creates a new {@link HttpContentCompressor} with the current settings.
     * 
     * @return a {@code HttpContentCompressor}
     */
    public HttpContentCompressor create() {
        // only support deflate & gzip in this version
        return new HttpContentCompressor(contentSizeThreshold,
                StandardCompressionOptions.deflate(compressionLevel, windowBits, memLevel),
                StandardCompressionOptions.gzip(compressionLevel, windowBits, memLevel));
    }

    @Override
    public String toString() {
        return "HttpContentCompressorFactory(compressionLevel=" + compressionLevel + ", windowBits=" + windowBits
                + ", memLevel=" + memLevel + ", contentSizeThreshold=" + contentSizeThreshold + ")";
    }

}
