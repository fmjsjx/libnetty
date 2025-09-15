package com.github.fmjsjx.libnetty.http.server.sse;

import com.github.fmjsjx.libnetty.http.server.DefaultHttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.fmjsjx.libnetty.http.server.Constants.SSE_EVENT_ENCODER;
import static com.github.fmjsjx.libnetty.http.server.Constants.TIMEOUT_HANDLER;
import static com.github.fmjsjx.libnetty.http.server.HttpServerHandler.READ_NEXT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.IDENTITY;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_EVENT_STREAM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

/**
 * The default implementation of {@link SseEventStream}.
 *
 * @author MJ Fang
 * @since 3.9
 */
class DefaultSseEventStream implements SseEventStream {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSseEventStream.class);
    private static final AttributeKey<SseEventStream> KEY_SSE_EVENT_STREAM = AttributeKey.valueOf("SseEventStream");

    private final HttpRequestContext ctx;
    private final Channel channel;
    private final Duration autoPingInterval;
    private Consumer<SseEventStream> onActiveAction;
    private BiConsumer<SseEventStream, Throwable> errorHandler;

    private final AtomicInteger state = new AtomicInteger(0);

    private final AtomicReference<ReadTimeoutHandler> timeoutHandlerRef = new AtomicReference<>();

    DefaultSseEventStream(HttpRequestContext ctx, Duration autoPingInterval, Consumer<SseEventStream> onActiveAction,
                          BiConsumer<SseEventStream, Throwable> errorHandler) {
        this.ctx = ctx;
        this.channel = ctx.channel();
        this.autoPingInterval = autoPingInterval;
        this.onActiveAction = onActiveAction;
        this.errorHandler = errorHandler;
    }

    @Override
    public boolean isOpen() {
        return !isClosed();
    }

    @Override
    public boolean isActive() {
        return state.get() == 1;
    }

    @Override
    public boolean isClosed() {
        return state.get() == 2;
    }

    @Override
    public SseEventStream onActive(Consumer<SseEventStream> action) {
        ensureOpen();
        onActiveAction = action;
        return this;
    }

    private void ensureOpen() {
        if (isClosed()) {
            throw new IllegalStateException("the event stream is already closed");
        }
        if (isActive()) {
            throw new IllegalStateException("the event stream is already active");
        }
    }

    @Override
    public SseEventStream onError(BiConsumer<SseEventStream, Throwable> errorHandler) {
        ensureOpen();
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public CompletableFuture<Void> sendEvent(Supplier<SseEventSerializable> eventSupplier) {
        if (isActive()) {
            var future = new CompletableFuture<Void>();
            ChannelFutureListener channelFutureListener = cf -> {
                if (cf.isSuccess()) {
                    future.complete(null);
                } else {
                    var cause = cf.cause();
                    future.completeExceptionally(cause);
                    closeWithError(cause);
                    cf.channel().close();
                }
            };
            var channel = this.channel;
            var eventLoop = channel.eventLoop();
            if (eventLoop.inEventLoop()) {
                channel.writeAndFlush(eventSupplier.get()).addListener(channelFutureListener);
            } else {
                eventLoop.execute(() -> channel.writeAndFlush(eventSupplier.get()).addListener(channelFutureListener));
            }
            return future;
        } else {
            // If not active, just skip and returns a completed future.
            return CompletableFuture.completedFuture(null);
        }
    }

    private void closeWithError(Throwable cause) {
        if (state.compareAndSet(1, 2)) {
            if (errorHandler != null) {
                try {
                    errorHandler.accept(this, cause);
                } catch (Exception e) {
                    logger.error("Unexpected error occurs when handle event-stream error: {}", cause, e);
                }
            }
        }
    }

    @Override
    public Duration autoPingInterval() {
        return autoPingInterval;
    }

    @Override
    public CompletableFuture<Void> close() {
        if (channel.eventLoop().inEventLoop()) {
            var cf = closeInEventLoop();
            if (cf != null) {
                var future = new CompletableFuture<Void>();
                cf.addListener(it -> future.complete(null));
                return future;
            }
            return CompletableFuture.completedFuture(null);
        }
        var future = new CompletableFuture<Void>();
        channel.eventLoop().execute(() -> {
            var cf = closeInEventLoop();
            if (cf != null) {
                cf.addListener(it -> future.complete(null));
            } else {
                future.complete(null);
            }
        });
        return future;
    }

    private ChannelFuture closeInEventLoop() {
        if (state.compareAndSet(1, 2)) {
            ChannelFutureListener resetOnSuccess = cf -> {
                if (cf.isSuccess()) {
                    var channel = cf.channel();
                    var timeoutHandler = timeoutHandlerRef.get();
                    if (timeoutHandler != null) {
                        channel.pipeline().addFirst(TIMEOUT_HANDLER, new ReadTimeoutHandler(timeoutHandler.getReaderIdleTimeInMillis(), TimeUnit.MILLISECONDS));
                    }
                    var attr = channel.attr(KEY_SSE_EVENT_STREAM);
                    attr.set(null);
                }
            };
            var channel = this.channel;
            return channel.writeAndFlush(EMPTY_LAST_CONTENT).addListeners(resetOnSuccess, READ_NEXT);
        }
        // do nothing but set closed
        state.set(2);
        return null;
    }

    @Override
    public CompletableFuture<HttpResult> start() {
        if (state.compareAndSet(0, 1)) {
            var future = new CompletableFuture<HttpResult>();
            ChannelFutureListener completeOnSuccess = cf -> {
                if (cf.isSuccess()) {
                    future.complete(new DefaultHttpResult(ctx, -1, OK));
                    if (autoPingInterval != null) {
                        new AutoPingTask().scheduleNext();
                    }
                    if (onActiveAction != null) {
                        onActiveAction.accept(this);
                    }
                } else {
                    var cause = cf.cause();
                    future.completeExceptionally(cause);
                    closeWithError(cause);
                    cf.channel().close();
                }
            };
            if (channel.eventLoop().inEventLoop()) {
                startInEventLoop().addListener(completeOnSuccess);
            } else {
                channel.eventLoop().execute(() -> {
                    try {
                        startInEventLoop().addListener(completeOnSuccess);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            }
            return future;
        }
        if (isActive()) {
            throw new IllegalStateException("the event stream is already active");
        }
        throw new IllegalStateException("the event stream is already closed");
    }

    private ChannelFuture startInEventLoop() {
        var channel = this.channel;
        var attr = channel.attr(KEY_SSE_EVENT_STREAM);
        if (attr.setIfAbsent(this) != null) {
            // There is already another event stream be active in the channel.
            state.set(2);
            throw new IllegalStateException("There is already another event stream be active in the channel.");
        }
        var pipeline = channel.pipeline();
        var timeoutHandler = pipeline.get(TIMEOUT_HANDLER);
        if (timeoutHandler instanceof ReadTimeoutHandler readTimeoutHandler) {
            pipeline.remove(TIMEOUT_HANDLER);
            timeoutHandlerRef.set(readTimeoutHandler);
        }
        if (pipeline.get(SSE_EVENT_ENCODER) == null) {
            pipeline.addLast(SSE_EVENT_ENCODER, SseEventEncoder.getInstance());
        }
        var response = ctx.responseFactory().create(OK);
        HttpUtil.setTransferEncodingChunked(response, true);
        response.headers().set(CONTENT_TYPE, TEXT_EVENT_STREAM);
        response.headers().set(CONTENT_ENCODING, IDENTITY);
        return channel.writeAndFlush(response);
    }

    private final class AutoPingTask implements Runnable {

        @Override
        public void run() {
            if (isActive()) {
                sendPing();
                scheduleNext();
            }
        }

        private void scheduleNext() {
            channel.eventLoop().schedule(this, autoPingInterval.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

}
