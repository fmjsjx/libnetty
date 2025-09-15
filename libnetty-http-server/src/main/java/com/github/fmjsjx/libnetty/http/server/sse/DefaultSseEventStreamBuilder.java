package com.github.fmjsjx.libnetty.http.server.sse;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.fmjsjx.libnetty.http.server.sse.SseConstants.DEFAULT_AUTO_PING_INTERVAL;

/**
 * The default implementation of {@link SseEventStreamBuilder}.
 *
 * @author MJ Fang
 * @since 3.9
 */
class DefaultSseEventStreamBuilder implements SseEventStreamBuilder {

    private final HttpRequestContext ctx;

    private Duration autoPingInterval;
    private Consumer<SseEventStream> onActiveAction;
    private BiConsumer<SseEventStream, Throwable> errorHandler;

    DefaultSseEventStreamBuilder(HttpRequestContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public SseEventStream build() {
        return new DefaultSseEventStream(ctx, autoPingInterval, onActiveAction, errorHandler);
    }

    @Override
    public SseEventStreamBuilder autoPing() {
        return autoPing(DEFAULT_AUTO_PING_INTERVAL);
    }

    @Override
    public SseEventStreamBuilder autoPing(Duration interval) {
        this.autoPingInterval = interval;
        return this;
    }

    @Override
    public SseEventStreamBuilder onActive(Consumer<SseEventStream> action) {
        this.onActiveAction = action;
        return this;
    }

    @Override
    public SseEventStreamBuilder onError(BiConsumer<SseEventStream, Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

}
