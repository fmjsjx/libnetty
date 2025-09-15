package com.github.fmjsjx.libnetty.http.server.sse;

import com.github.fmjsjx.libnetty.http.server.HttpResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Interface defines methods of SSE event stream.
 *
 * @author MJ Fang
 * @since 3.9
 */
public interface SseEventStream {

    /**
     * Returns {@code true} if this event stream is open and may get active later.
     * <p>
     * This method is equivalent to <pre>{@code
     * !isClosed();
     * }</pre>.
     *
     * @return {@code true} if this event stream is open
     */
    boolean isOpen();

    /**
     * Returns {@code true} if this event stream is active.
     *
     * @return {@code true} if this event stream is active
     */
    boolean isActive();

    /**
     * Returns {@code true} if this event stream is closed.
     * <p>
     * This method is equivalent to <pre>{@code
     * !isOpen();
     * }</pre>.
     *
     * @return {@code true} if this event stream is closed
     */
    boolean isClosed();

    /**
     * Add the action that will be executed when the event stream is just
     * active.
     *
     * @param action the action
     * @return this event stream
     */
    SseEventStream onActive(Consumer<SseEventStream> action);

    /**
     * Add the handler handle error.
     * <p>
     * Note that the error handler will be triggered at most once during
     * the entire lifecycle of the event stream, after which the stream
     * will be automatically closed.
     *
     * @param errorHandler the handler handle error
     * @return this event stream
     */
    SseEventStream onError(BiConsumer<SseEventStream, Throwable> errorHandler);

    /**
     * Send the event built by the specified {@link SseEventBuilder} to
     * this stream.
     *
     * @param eventBuilder the {@link SseEventBuilder} builds the event
     * @return the {@code CompletableFuture<Void>}
     */
    default CompletableFuture<Void> sendEvent(SseEventBuilder eventBuilder) {
        return sendEvent(eventBuilder::build);
    }

    /**
     * Send the specified event to this stream.
     *
     * @param event the SSE event
     * @return the {@code CompletableFuture<Void>}
     */
    default CompletableFuture<Void> sendEvent(SseEventSerializable event) {
        return sendEvent(event.toSupplier());
    }

    /**
     * Send the specified event to this stream.
     *
     * @param eventSupplier the SSE event supplier
     * @return the {@code CompletableFuture<Void>}
     */
    CompletableFuture<Void> sendEvent(Supplier<SseEventSerializable> eventSupplier);

    /**
     * Send the ping event, {@code "event: ping\n"}, to this stream.
     *
     * @return the {@code CompletableFuture<Void>}
     */
    default CompletableFuture<Void> sendPing() {
        return sendEvent(SseEventBuilder.pingEvent());
    }

    /**
     * Returns the interval duration between each ping event
     * be sent automatically.
     * <p>
     * May be {@code null} if ping events will never be sent
     * automatically.
     *
     * @return the interval duration
     */
    default Duration autoPingInterval() {
        return null;
    }

    /**
     * Send the last HTTP content {@code CRLF} to the client and close
     * this stream.
     *
     * @return the {@code CompletableFuture<Void>}
     */
    CompletableFuture<Void> close();

    /**
     * Send HTTP response with status {@code 200 OK} and header
     * {@code content-type: event-stream} to the client and returns the
     * {@link HttpResult} asynchronously.
     * <p>
     * This event stream will get active at an appropriate time.
     *
     * @return a {@code CompletableFuture<HttpResult>}
     */
    CompletableFuture<HttpResult> start();

}
