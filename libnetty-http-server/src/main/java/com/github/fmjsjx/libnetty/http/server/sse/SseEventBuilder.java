package com.github.fmjsjx.libnetty.http.server.sse;

/**
 * Interface builds SSE events.
 *
 * @author MJ Fang
 * @since 3.9
 */
public interface SseEventBuilder {

    /**
     * Returns the SSE ping event.
     *
     * @return the SSE ping event
     */
    static SseEventSerializable pingEvent() {
        return ping().build();
    }

    /**
     * Returns a {@link SseEventBuilder} builds SSE ping events.
     *
     * @return a {@code SseEventBuilder} builds SSE ping events
     */
    static SseEventBuilder ping() {
        return PingEventSseEventBuilder.INSTANCE;
    }

    /**
     * Returns a new {@link SseEventBuilder} with the specified
     * {@code data} given.
     *
     * @param data the data part of the SSE event
     * @return a new {@code SseEventBuilder} with the specified
     * {@code data} given
     */
    static SseEventBuilder message(CharSequence data) {
        return create().data(data);
    }

    /**
     * Creates and returns a new {@link SseEventBuilder}.
     *
     * @return a new {@code SseEventBuilder}
     */
    static SseEventBuilder create() {
        return new SseEventBuilderImpl();
    }

    /**
     * Sets the event.
     *
     * @param event the event
     * @return this builder
     */
    SseEventBuilder event(CharSequence event);

    /**
     * Sets the ID.
     *
     * @param id the ID
     * @return this builder
     */
    SseEventBuilder id(Object id);

    /**
     * Sets the data.
     *
     * @param data the data
     * @return this builder
     */
    SseEventBuilder data(CharSequence data);

    /**
     * Build and returns a new SSE event implement
     * {@link SseEventSerializable}.
     *
     * @return a new SSE event implement {@link SseEventSerializable}
     */
    SseEventSerializable build();

}
