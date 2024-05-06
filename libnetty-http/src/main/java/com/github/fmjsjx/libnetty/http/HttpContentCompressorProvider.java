package com.github.fmjsjx.libnetty.http;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.netty.handler.codec.compression.BrotliOptions;
import io.netty.handler.codec.compression.DeflateOptions;
import io.netty.handler.codec.compression.GzipOptions;
import io.netty.handler.codec.compression.ZstdOptions;
import io.netty.handler.codec.http.HttpContentCompressor;

/**
 * Interface to provide {@link HttpContentCompressor}s.
 * 
 * @since 2.6
 *
 * @author MJ Fang
 * 
 * @see HttpContentCompressor
 */
@FunctionalInterface
public interface HttpContentCompressorProvider extends Supplier<HttpContentCompressor> {

    /**
     * Returns an apply action that just create builder with default options but do
     * nothing.
     * 
     * @return a {@code Consumer<HttpContentCompressorProvider.Builder>}
     */
    static Consumer<Builder> defaultOptions() {
        return builder -> {
            // just create builder with default settings
        };
    }

    /**
     * Creates a default {@link HttpContentCompressorProvider.Builder} instance.
     * 
     * @return a {@code HttpContentCompressorProvider.Builder}
     */
    static Builder builder() {
        return DefaultHttpContentCompressorProvider.builder();
    }

    /**
     * Creates and returns a new {@link HttpContentCompressor} instance.
     * 
     * @return a new {@code HttpContentCompressor} instance
     */
    HttpContentCompressor create();

    /**
     * Get a new {@link HttpContentCompressor} instance.
     * <p>
     * This method is equivalent to {@link #create}.
     * 
     * @return a new {@code HttpContentCompressor} instance
     */
    @Override
    default HttpContentCompressor get() {
        return create();
    }

    /**
     * Interface to build {@link HttpContentCompressorProvider}.
     */
    interface Builder {

        /**
         * Creates a new {@link HttpContentCompressorProvider} with the current
         * settings.
         * 
         * @return a {@code HttpContentCompressorProvider}
         */
        HttpContentCompressorProvider build();

        /**
         * Set the content size threshold. The response body is compressed when the size
         * of the response body exceeds the threshold. The value should be a non-negative
         * number. {@code 0} will enable compression for all responses. The default value
         * is {@code 1024}.
         * 
         * @param contentSizeThreshold the content size threshold
         * @return this builder
         */
        Builder contentSizeThreshold(int contentSizeThreshold);

        /**
         * Enable gzip with default options.
         * 
         * @return this builder
         */
        Builder gzip();

        /**
         * Enable gzip with given options.
         * 
         * @param options {@link GzipOptions}
         * @return this builder
         */
        Builder gzip(GzipOptions options);

        /**
         * Disable gzip.
         * 
         * @return this builder
         */
        Builder disableGzip();

        /**
         * Enable deflate with default options.
         * 
         * @return this builder
         */
        Builder deflate();

        /**
         * Enable deflate with given options.
         * 
         * @param options {@link DeflateOptions}
         * @return this builder
         */
        Builder deflate(DeflateOptions options);

        /**
         * Disable deflate.
         * 
         * @return this builder
         */
        Builder disableDeflate();

        /**
         * Enable brotli with default options.
         * 
         * @return this builder
         */
        Builder brotli();

        /**
         * Enable brotli with given options.
         * 
         * @param options {@link BrotliOptions}
         * @return this builder
         */
        Builder brotli(BrotliOptions options);

        /**
         * Disable brotli.
         * 
         * @return this builder
         */
        Builder disableBrotli();

        /**
         * Enable zstd with default options.
         * 
         * @return this builder
         */
        Builder zstd();

        /**
         * Enable brotli with given options.
         * 
         * @param options {@link ZstdOptions}
         * @return this builder
         */
        Builder zstd(ZstdOptions options);

        /**
         * Disable brotli.
         * 
         * @return this builder
         */
        Builder disableZstd();

    }

}
