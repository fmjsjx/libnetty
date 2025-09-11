package com.github.fmjsjx.libnetty.http.server.sse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static com.github.fmjsjx.libnetty.http.server.sse.SseConstants.EVENT_PING;

/**
 * The {@link SseEventBuilder} builds ping events.
 *
 * @author MJ Fang
 * @see SseEventBuilder
 * @since 3.9
 */
class PingEventSseEventBuilder implements SseEventBuilder {

    static final PingEventSseEventBuilder INSTANCE = new PingEventSseEventBuilder();

    private static final class PingEventSseEvent implements SseEventSerializable {

        private static final PingEventSseEvent INSTANCE = new PingEventSseEvent();

        @Override
        public void serialize(ByteBuf out) {
            out.writeBytes(EVENT_PING.duplicate());
        }

        @Override
        public ByteBuf serialize(ByteBufAllocator allocator) {
            return EVENT_PING.duplicate();
        }

        @Override
        public String toString() {
            return "{event: ping}";
        }

        private PingEventSseEvent() {
        }

    }

    @Override
    public SseEventBuilder event(CharSequence event) {
        return this;
    }

    @Override
    public SseEventBuilder id(Object id) {
        return this;
    }

    @Override
    public SseEventBuilder data(CharSequence data) {
        return this;
    }

    @Override
    public SseEventSerializable build() {
        return PingEventSseEvent.INSTANCE;
    }

    private PingEventSseEventBuilder() {
    }

}
