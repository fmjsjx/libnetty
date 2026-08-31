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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

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

    /**
     * Returns a {@link ChunkedHttpContentHandler} which streams the HTTP content
     * line by line as a {@link Flux} of {@code String}s converted using the
     * default character set {@code UTF-8}.
     *
     * <p>The returned {@link Flux} traverses the underlying blocking
     * {@link Stream} of lines on a dedicated virtual thread and therefore never
     * blocks the subscribing thread.</p>
     *
     * @return a {@link ChunkedHttpContentHandler}
     * @since 4.3
     * @see #ofLines()
     */
    public static ChunkedHttpContentHandler<Flux<String>> ofLinesFlux() {
        return new LineFluxHttpContentHandler(ofLines());
    }

    /**
     * Returns a {@link ChunkedHttpContentHandler} which streams the HTTP content
     * line by line as a {@link Flux} of {@code String}s converted using the
     * given {@code charset}.
     *
     * <p>The returned {@link Flux} traverses the underlying blocking
     * {@link Stream} of lines on a dedicated virtual thread and therefore never
     * blocks the subscribing thread.</p>
     *
     * @param charset the character set to convert the lines with
     * @return a {@link ChunkedHttpContentHandler}
     * @since 4.3
     * @see #ofLines(Charset)
     */
    public static ChunkedHttpContentHandler<Flux<String>> ofLinesFlux(Charset charset) {
        return new LineFluxHttpContentHandler(ofLines(charset));
    }

    /**
     * Returns a {@link ChunkedHttpContentHandler} which streams the HTTP content
     * line by line as a {@link Flux} of {@code String}s converted using the
     * given {@code charset}.
     *
     * <p>The returned {@link Flux} is created via {@link Flux#create} with the
     * {@link FluxSink.OverflowStrategy#BUFFER BUFFER} overflow strategy and
     * traverses the underlying blocking {@link Stream} of lines on a dedicated
     * virtual thread, so that neither the subscribing thread nor any platform
     * carrier thread is blocked. When the {@link Flux} is cancelled, completed
     * or terminated with an error, the underlying {@link Stream} is closed and
     * the traversing virtual thread is interrupted to release the resources as
     * soon as possible.</p>
     *
     * <p>Note that the returned {@link Flux} must be subscribed at most once as
     * the underlying {@link Stream} can be traversed only once, and the
     * backpressure can only be applied on the consuming side since the HTTP
     * content is always pushed by the client once received.</p>
     *
     * @param charset        the character set to convert the lines with
     * @param bufferCapacity the buffer capacity of the internal queue
     * @return a {@link ChunkedHttpContentHandler}
     * @since 4.3
     * @see #ofLines(Charset, int)
     */
    public static ChunkedHttpContentHandler<Flux<String>> ofLinesFlux(Charset charset, int bufferCapacity) {
        return new LineFluxHttpContentHandler(ofLines(charset, bufferCapacity));
    }

    private HttpContentHandlers() {
    }

    private static final class LineStreamHttpContentHandler implements ChunkedHttpContentHandler<Stream<String>> {

        private static final int DEFAULT_BUFFER_CAPACITY = 128;

        private static final VarHandle LINE_STREAM_VAR;
        private static final Object INITIALIZING_TOKEN = new Object();
        private static final Object EOF = new Object();

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
                    return switch (obj) {
                        case String str -> str;
                        case RuntimeException e -> throw e;
                        default -> null;
                    };
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }).takeWhile(Objects::nonNull);
        }

        @Override
        public void onComplete() {
            queue.offer(EOF);
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

    private static final class LineFluxHttpContentHandler implements ChunkedHttpContentHandler<Flux<String>> {

        private static final AtomicLong THREAD_COUNTER = new AtomicLong();

        private final ChunkedHttpContentHandler<Stream<String>> handler;
        private final Flux<String> flux;

        private LineFluxHttpContentHandler(ChunkedHttpContentHandler<Stream<String>> handler) {
            this.handler = handler;
            this.flux = Flux.create(this::subscribe, FluxSink.OverflowStrategy.BUFFER);
        }

        @Override
        public void accept(ByteBuf content) {
            handler.accept(content);
        }

        @Override
        public Flux<String> get() {
            return flux;
        }

        @Override
        public void onComplete() {
            handler.onComplete();
        }

        @Override
        public void onError(Throwable cause) {
            handler.onError(cause);
        }

        private void subscribe(FluxSink<String> sink) {
            var stream = handler.get();
            var threadRef = new AtomicReference<Thread>();
            sink.onDispose(() -> {
                var thread = threadRef.get();
                if (thread != null) {
                    // Interrupt the virtual thread to stop traversing the
                    // blocking stream as soon as possible
                    thread.interrupt();
                }
            });
            var thread = Thread.ofVirtual()
                    .name("libnetty-http-client-lines-", THREAD_COUNTER.incrementAndGet())
                    .unstarted(() -> {
                        try (stream) {
                            if (!sink.isCancelled()) {
                                stream.takeWhile(ignored -> !sink.isCancelled()).forEach(sink::next);
                                sink.complete();
                            }
                        } catch (Throwable cause) {
                            if (!sink.isCancelled()) {
                                sink.error(cause);
                            }
                        }
                    });
            threadRef.set(thread);
            thread.start();
        }

    }

}
