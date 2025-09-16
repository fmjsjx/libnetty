package com.github.fmjsjx.libnetty.http.server.sse;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A Builder builds {@link SseEventStream}.
 *
 * @author MJ Fang
 * @since 3.9
 */
public interface SseEventStreamBuilder {

    /**
     * Creates and returns a new {@link SseEventStreamBuilder} with the
     * specified {@link HttpRequestContext} given.
     *
     * @param ctx the {@link HttpRequestContext}
     * @return a new {@link SseEventStreamBuilder} with the specified
     * {@link HttpRequestContext} given.
     */
    static SseEventStreamBuilder create(HttpRequestContext ctx) {
        return new DefaultSseEventStreamBuilder(ctx);
    }

    /**
     * Build a new {@link SseEventStream}.
     *
     * @return a new {@code SseEventStream}
     */
    SseEventStream build();

    /**
     * Enable auto-ping with the default interval duration.
     *
     * @return this builder
     */
    SseEventStreamBuilder autoPing();

    /**
     * Enable auto-ping with the specified interval duration, or disable
     * auto-ping when the specified interval duration is {@code null}.
     *
     * @param interval the interval duration
     * @return this builder
     */
    SseEventStreamBuilder autoPing(Duration interval);

    /**
     * Enable auto-ping with the specified interval amount in the
     * specified unit.
     *
     * @param interval the amount of the interval duration
     * @param unit     the unit that the duration is measured in
     * @return this builder
     */
    default SseEventStreamBuilder autoPing(long interval, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit must not be null");
        return autoPing(Duration.of(interval, unit.toChronoUnit()));
    }

    /**
     * Disable auto-ping.
     *
     * @return this builder
     */
    default SseEventStreamBuilder disableAutoPing() {
        return autoPing(null);
    }

    /**
     * Add the action that will be executed when the event stream is just
     * active.
     *
     * @param action the action
     * @return this builder
     * @see SseEventStream#onActive(Consumer)
     */
    SseEventStreamBuilder onActive(Consumer<SseEventStream> action);

    /**
     * Add the handler handle error.
     * <p>
     * Note that the error handler will be triggered at most once during
     * the entire lifecycle of the event stream, after which the stream
     * will be automatically closed.
     *
     * @param errorHandler the handler handle error
     * @return this builder
     * @see SseEventStream#onError(BiConsumer)
     */
    SseEventStreamBuilder onError(BiConsumer<SseEventStream, Throwable> errorHandler);

}
