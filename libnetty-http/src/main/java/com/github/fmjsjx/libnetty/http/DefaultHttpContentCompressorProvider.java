package com.github.fmjsjx.libnetty.http;

import io.netty.handler.codec.compression.*;
import io.netty.handler.codec.http.HttpContentCompressor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

/**
 * The default implementation of {@link HttpContentCompressorProvider}.
 *
 * @author MJ Fang
 * @see HttpContentCompressor
 * @see HttpContentCompressorProvider
 * @since 2.6
 */
public class DefaultHttpContentCompressorProvider implements HttpContentCompressorProvider {

    private static final int DEFAULT_CONTENT_SIZE_THRESHOLD = 1024;

    /**
     * Creates a new {@link DefaultHttpContentCompressorProvider.Builder} instance.
     *
     * @return a {@code DefaultHttpContentCompressorProvider.Builder}
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * A builder to build {@link HttpContentCompressorProvider}s.
     *
     * @see HttpContentCompressorProvider.Builder
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static final class Builder implements HttpContentCompressorProvider.Builder {

        private int contentSizeThreshold = DEFAULT_CONTENT_SIZE_THRESHOLD;

        private Optional<GzipOptions> gzipOptions = Optional.of(StandardCompressionOptions.gzip());
        private Optional<DeflateOptions> deflateOptions = Optional.of(StandardCompressionOptions.deflate());
        // Since 4.1, snappy is supported
        private Optional<SnappyOptions> snappyOptions = Optional.of(StandardCompressionOptions.snappy());
        private Optional<BrotliOptions> brotliOptions = Brotli.isAvailable()
                ? Optional.of(StandardCompressionOptions.brotli())
                : Optional.empty();
        private Optional<ZstdOptions> zstdOptions = Zstd.isAvailable() ? Optional.of(StandardCompressionOptions.zstd())
                : Optional.empty();

        private Builder() {
        }

        @Override
        public Builder contentSizeThreshold(int contentSizeThreshold) {
            this.contentSizeThreshold = contentSizeThreshold;
            return this;
        }

        @Override
        public DefaultHttpContentCompressorProvider build() {
            var list = new ArrayList<CompressionOptions>(4);
            gzipOptions.ifPresent(list::add);
            deflateOptions.ifPresent(list::add);
            brotliOptions.ifPresent(list::add);
            zstdOptions.ifPresent(list::add);
            if (list.isEmpty()) {
                return null;
            }
            var contentSizeThreshold = Math.max(0, this.contentSizeThreshold);
            return new DefaultHttpContentCompressorProvider(contentSizeThreshold,
                    list.toArray(CompressionOptions[]::new));
        }

        @Override
        public Builder gzip() {
            if (gzipOptions.isEmpty()) {
                gzipOptions = Optional.of(StandardCompressionOptions.gzip());
            }
            return this;
        }

        @Override
        public Builder gzip(GzipOptions options) {
            gzipOptions = Optional.ofNullable(options);
            return this;
        }

        @Override
        public Builder disableGzip() {
            gzipOptions = Optional.empty();
            return this;
        }

        @Override
        public Builder deflate() {
            if (deflateOptions.isEmpty()) {
                deflateOptions = Optional.of(StandardCompressionOptions.deflate());
            }
            return this;
        }

        @Override
        public Builder deflate(DeflateOptions options) {
            deflateOptions = Optional.ofNullable(options);
            return this;
        }

        @Override
        public Builder disableDeflate() {
            deflateOptions = Optional.empty();
            return this;
        }

        @Override
        public HttpContentCompressorProvider.Builder snappy() {
            if (snappyOptions.isEmpty()) {
                snappyOptions = Optional.of(StandardCompressionOptions.snappy());
            }
            return this;
        }

        @Override
        public HttpContentCompressorProvider.Builder snappy(SnappyOptions options) {
            snappyOptions = Optional.ofNullable(options);
            return this;
        }

        @Override
        public Builder disableSnappy() {
            snappyOptions = Optional.empty();
            return this;
        }

        @Override
        public Builder brotli() {
            if (brotliOptions.isEmpty() && Brotli.isAvailable()) {
                brotliOptions = Optional.of(StandardCompressionOptions.brotli());
            }
            return this;
        }

        @Override
        public Builder brotli(BrotliOptions options) {
            brotliOptions = Optional.ofNullable(options);
            return this;
        }

        @Override
        public Builder disableBrotli() {
            brotliOptions = Optional.empty();
            return this;
        }

        @Override
        public Builder zstd() {
            if (zstdOptions.isEmpty() && Zstd.isAvailable()) {
                zstdOptions = Optional.of(StandardCompressionOptions.zstd());
            }
            return this;
        }

        @Override
        public Builder zstd(ZstdOptions options) {
            zstdOptions = Optional.ofNullable(options);
            return this;
        }

        @Override
        public Builder disableZstd() {
            zstdOptions = Optional.empty();
            return this;
        }

    }

    private final int contentSizeThreshold;
    private final CompressionOptions[] compressionOptions;

    /**
     * Create a new {@link DefaultHttpContentCompressorProvider} instance with
     * specified {@link CompressionOptions}s
     *
     * @param contentSizeThreshold The response body is compressed when the size of
     *                             the response body exceeds the threshold. The
     *                             value should be a non-negative number. {@code 0}
     *                             will enable compression for all responses.
     * @param compressionOptions   {@link CompressionOptions} or {@code null} if the
     *                             default should be used.
     */
    public DefaultHttpContentCompressorProvider(int contentSizeThreshold, CompressionOptions... compressionOptions) {
        this.contentSizeThreshold = contentSizeThreshold;
        this.compressionOptions = Arrays.copyOf(compressionOptions, compressionOptions.length);
    }

    @Override
    public HttpContentCompressor create() {
        return new HttpContentCompressor(contentSizeThreshold, compressionOptions);
    }

}
