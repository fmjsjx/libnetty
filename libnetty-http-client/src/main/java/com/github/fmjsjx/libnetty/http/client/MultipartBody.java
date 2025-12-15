package com.github.fmjsjx.libnetty.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.ErrorDataEncoderException;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An HTTP request body class holding components of {@code multipart/form-data}.
 *
 * @since 3.0
 */
public final class MultipartBody {

    private final Charset charset;
    private final List<DataEntry> entries;
    private List<HttpData> toDeleteDataList;

    MultipartBody(Charset charset, List<DataEntry> entries) {
        this.charset = charset;
        this.entries = List.copyOf(entries);
    }

    /**
     * Returns the charset.
     *
     * @return the charset
     */
    public Charset charset() {
        return charset;
    }

    List<DataEntry> entries() {
        return entries;
    }

    List<HttpData> getToDeleteDataList(boolean createIfNull) {
        var toDeleteDataList = this.toDeleteDataList;
        if (createIfNull && toDeleteDataList == null) {
            this.toDeleteDataList = toDeleteDataList = new ArrayList<>();
        }
        return toDeleteDataList;
    }

    List<HttpData> getToDeleteDataList() {
        return getToDeleteDataList(false);
    }

    @Override
    public String toString() {
        return "MultipartBody(charset=" + charset + ", entries=" + entries + ")";
    }

    /**
     * Returns a new {@link Builder} instance to build {@link MultipartBody}.
     *
     * @return a new {@code Builder} instance
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * The builder class to build {@link MultipartBody}s.
     */
    public static final class Builder {

        private Charset charset = HttpConstants.DEFAULT_CHARSET;
        private final List<DataEntry> entries = new ArrayList<>();

        /**
         * Constructs a new {@link Builder} instance,
         */
        public Builder() {
        }

        /**
         * Sets the charset.
         *
         * @param charset the charset
         * @return this builder
         */
        public Builder charset(Charset charset) {
            this.charset = charset == null ? HttpConstants.DEFAULT_CHARSET : charset;
            return this;
        }

        /**
         * Add the entry into this builder.
         *
         * @param entry the entry
         * @return this builder
         */
        Builder addEntry(DataEntry entry) {
            entries.add(entry);
            return this;
        }

        /**
         * Add the attribute entry with the specified name and the specified value given.
         *
         * @param name  the name
         * @param value the value
         * @return this builder
         */
        public Builder addAttribute(String name, String value) {
            Objects.requireNonNull(name, "name must not be null");
            return addEntry(new AttributeEntry(name, value));
        }

        /**
         * Add the file upload entry with the content type {@code "application/octet-stream"} and the specified
         * parameters given.
         *
         * @param name the name
         * @param file the file
         * @return this builder
         */
        public Builder addFileUpload(String name, File file) {
            return addFileUpload(name, file.getName(), file, null);
        }

        /**
         * Add the file upload entry with the specified parameters given.
         *
         * @param name        the name
         * @param file        the file
         * @param contentType the content type, nullable, if {@code null} then use {@code "application/octet-stream"}
         * @return this builder
         */
        public Builder addFileUpload(String name, File file, String contentType) {
            return addFileUpload(name, file.getName(), file, contentType);
        }

        /**
         * Add the file upload entry with the specified parameters given.
         *
         * @param name        the name
         * @param filename    the filename
         * @param file        the file
         * @param contentType the content type, nullable, if {@code null} then use {@code "application/octet-stream"}
         * @return this builder
         */
        public Builder addFileUpload(String name, String filename, File file, String contentType) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(filename, "filename must not be null");
            Objects.requireNonNull(file, "file must not be null");
            return addEntry(new FileUploadEntry(name, filename, file,
                    contentType == null ? "application/octet-stream" : contentType));
        }

        /**
         * Add the file upload entry with the specified parameters given.
         *
         * @param name            the name
         * @param filename        the filename
         * @param contentProvider the file content provider
         * @return this builder
         * @author MJ Fang
         * @since 3.7
         */
        public Builder addFileUpload(String name, String filename, Function<ByteBufAllocator, ByteBuf> contentProvider) {
            return addFileUpload(name, filename, null, contentProvider);
        }

        /**
         * Add the file upload entry with the specified parameters given.
         *
         * @param name            the name
         * @param filename        the filename
         * @param contentType     the content type, nullable, if {@code null}
         *                        then use {@code "application/octet-stream"}
         * @param contentProvider the file content provider
         * @return this builder
         * @author MJ Fang
         * @since 3.7
         */
        public Builder addFileUpload(String name, String filename, String contentType,
                                     Function<ByteBufAllocator, ByteBuf> contentProvider) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(filename, "filename must not be null");
            Objects.requireNonNull(contentProvider, "fileContentProvider must not be null");
            var entry = new ContentProviderFileUploadEntry(name, filename,
                    contentType == null ? "application/octet-stream" : contentType, contentProvider);
            return addEntry(entry);
        }

        /**
         * Add the file upload entry with the specified parameters given.
         *
         * @param name            the name
         * @param filename        the filename
         * @param contentSupplier the file content supplier
         * @return this builder
         * @author MJ Fang
         * @since 3.7
         */
        public Builder addFileUpload(String name, String filename, Supplier<ByteBuf> contentSupplier) {
            return addFileUpload(name, filename, null, contentSupplier);
        }

        /**
         * Add the file upload entry with the specified parameters given.
         *
         * @param name            the name
         * @param filename        the filename
         * @param contentType     the content type, nullable, if {@code null}
         *                        then use {@code "application/octet-stream"}
         * @param contentSupplier the file content supplier
         * @return this builder
         * @author MJ Fang
         * @since 3.7
         */
        public Builder addFileUpload(String name, String filename, String contentType,
                                     Supplier<ByteBuf> contentSupplier) {
            return addFileUpload(name, filename, contentType, alloc -> contentSupplier.get());
        }

        /**
         * Build a new {@link MultipartBody} instance.
         *
         * @return a new {@code MultipartBody} instance
         */
        public MultipartBody build() {
            return new MultipartBody(charset, entries);
        }

    }

}

interface DataEntry {

    String name();

    default void addBody(HttpPostRequestEncoder encoder) throws ErrorDataEncoderException {
        throw new UnsupportedOperationException();
    }

}

record AttributeEntry(String name, String value) implements DataEntry {
    @Override
    public void addBody(HttpPostRequestEncoder encoder) throws ErrorDataEncoderException {
        encoder.addBodyAttribute(name, value);
    }
}

record FileUploadEntry(String name, String filename, File file, String contentType) implements DataEntry {
    @Override
    public void addBody(HttpPostRequestEncoder encoder) throws ErrorDataEncoderException {
        encoder.addBodyFileUpload(name, filename, file, contentType, false);
    }
}

record ContentProviderFileUploadEntry(String name, String filename, String contentType,
                                      Function<ByteBufAllocator, ByteBuf> contentProvider) implements DataEntry {
}
