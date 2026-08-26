package com.github.fmjsjx.libnetty.http.client;

import com.github.fmjsjx.libnetty.http.client.util.DynamicHybridBlockingQueue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Implementations of {@link HttpContentHandler}.
 *
 * @author MJ Fang
 * @since 1.0
 */
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


    /**
     * Returns a {@link ChunkedHttpContentHandler} which streams the HTTP content
     * line by line as a {@link Stream} of {@code String}s converted using the
     * default character set {@code UTF-8}.
     *
     * @return a {@link ChunkedHttpContentHandler}
     * @since 4.3
     */
    public static ChunkedHttpContentHandler<Stream<String>> ofLines() {
        return ofLines(CharsetUtil.UTF_8);
    }

    /**
     * Returns a {@link ChunkedHttpContentHandler} which streams the HTTP content
     * line by line as a {@link Stream} of {@code String}s converted using the
     * given {@code charset}.
     *
     * @param charset the character set to convert the lines with
     * @return a {@link ChunkedHttpContentHandler}
     * @since 4.3
     */
    public static ChunkedHttpContentHandler<Stream<String>> ofLines(Charset charset) {
        return new LineStreamHttpContentHandler(charset);
    }

    /**
     * Returns a {@link ChunkedHttpContentHandler} which streams the HTTP content
     * line by line as a {@link Stream} of {@code String}s converted using the
     * given {@code charset}.
     *
     * @param charset        the character set to convert the lines with
     * @param bufferCapacity the buffer capacity of the internal queue
     * @return a {@link ChunkedHttpContentHandler}
     * @since 4.3
     */
    public static ChunkedHttpContentHandler<Stream<String>> ofLines(Charset charset, int bufferCapacity) {
        return new LineStreamHttpContentHandler(charset, bufferCapacity);
    }

    private HttpContentHandlers() {
    }

    private static final class LineStreamHttpContentHandler implements ChunkedHttpContentHandler<Stream<String>> {

        private static final int DEFAULT_BUFFER_CAPACITY = 128;

        private static final VarHandle LINE_STREAM_VAR;
        private static final Object INITIALIZING_TOKEN = new Object();

        static {
            try {
                LINE_STREAM_VAR = MethodHandles.lookup().findVarHandle(LineStreamHttpContentHandler.class, "lineStream", Object.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private final Charset charset;
        private final DynamicHybridBlockingQueue<Object> queue;

        @SuppressWarnings("unused")
        private volatile Object lineStream;

        private StringBuilder lineBuffer;

        private LineStreamHttpContentHandler(Charset charset) {
            this(charset, DEFAULT_BUFFER_CAPACITY);
        }

        private LineStreamHttpContentHandler(Charset charset, int bufferCapacity) {
            this.charset = charset;
            this.queue = new DynamicHybridBlockingQueue<>(bufferCapacity);
        }

        @Override
        public void accept(ByteBuf content) {
            var writerIndex = content.writerIndex();
            for (var readerIndex = content.readerIndex(); readerIndex < writerIndex; ) {
                var length = writerIndex - readerIndex;
                var lfIndex = content.forEachByte(readerIndex, length, ByteProcessor.FIND_LF);
                if (lfIndex == -1) {
                    var lineRemaining = content.toString(readerIndex, length, charset);
                    var lineBuffer = this.lineBuffer;
                    if (lineBuffer == null) {
                        this.lineBuffer = lineBuffer = new StringBuilder();
                    }
                    lineBuffer.append(lineRemaining);
                    break;
                } else {
                    var line = content.toString(readerIndex, lfIndex - readerIndex + 1, charset);
                    readerIndex = lfIndex + 1;
                    var lineBuffer = this.lineBuffer;
                    if (lineBuffer != null) {
                        queue.offer(lineBuffer.append(line).toString());
                        this.lineBuffer = null;
                    } else {
                        queue.offer(line);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<String> get() {
            for (; ; ) {
                Object current = LINE_STREAM_VAR.getAcquire(this);
                if (current != null) {
                    if (current == INITIALIZING_TOKEN) {
                        Thread.onSpinWait();
                    } else {
                        return (Stream<String>) current;
                    }
                } else if (LINE_STREAM_VAR.compareAndSet(this, null, INITIALIZING_TOKEN)) {
                    try {
                        var lineStream = createLineStream();
                        LINE_STREAM_VAR.setRelease(this, lineStream);
                        return lineStream;
                    } catch (Throwable cause) {
                        // Reset to null if initialization failed
                        LINE_STREAM_VAR.setRelease(this, null);
                        throw cause;
                    }
                }
            }
        }

        private Stream<String> createLineStream() {
            return Stream.generate(() -> {
                try {
                    var obj = queue.take();
                    if (obj instanceof String str) {
                        return str;
                    }
                    if (obj instanceof RuntimeException e) {
                        throw e;
                    }
                    return null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }).takeWhile(Objects::nonNull);
        }

        @Override
        public void onComplete() {
            queue.offer(null);
        }

        @Override
        public void onError(Throwable cause) {
            if (cause instanceof RuntimeException e) {
                queue.offer(e);
            } else {
                queue.offer(new RuntimeException(cause));
            }
        }
    }

}
