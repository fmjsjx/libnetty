package com.github.fmjsjx.libnetty.http.server.sse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.AsciiString;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.fmjsjx.libnetty.http.server.sse.SseConstants.*;
import static io.netty.handler.codec.http.HttpConstants.LF;

/**
 * Package protected implementation of {@link SseEventBuilder}.
 *
 * @author MJ Fang
 * @since 3.9
 */
class SseEventBuilderImpl implements SseEventBuilder {

    private CharSequence event;
    private Object id;
    private CharSequence data;

    @Override
    public SseEventBuilder event(CharSequence event) {
        this.event = event;
        return this;
    }

    @Override
    public SseEventBuilder id(Object id) {
        this.id = id;
        return this;
    }

    @Override
    public SseEventBuilder data(CharSequence data) {
        this.data = data;
        return this;
    }

    @Override
    public SseEventSerializable build() {
        var data = this.data;
        var event = this.event;
        var id = this.id;
        if (data != null) {
            if (event == null && id == null) {
                return new MessageSseEvent(data);
            }
            return new DefaultSseEvent(event, id, data);
        }
        if (event != null) {
            if (id == null) {
                return new EventOnlySseEvent(event);
            }
            return new DefaultSseEvent(event, id, null);
        }
        if (id != null) {
            return new DefaultSseEvent(null, id, null);
        }
        throw new IllegalArgumentException("Can't build SSE event without any value.");
    }

    abstract static class AbstractSseEvent implements SseEventSerializable {

        static ByteBuf writeCharSequence(ByteBuf out, CharSequence value) {
            return writeValue(out, value);
        }

        static ByteBuf writeValue(ByteBuf out, Object value) {
            if (value instanceof AsciiString ascii) {
                ByteBufUtil.writeAscii(out, ascii);
            } else {
                out.writeBytes(value.toString().getBytes(StandardCharsets.UTF_8));
            }
            return out;
        }

        static void writeLF(ByteBuf out) {
            out.writeByte(LF);
        }

        static void writeData(ByteBuf out, CharSequence data) {
            out.writeBytes(LABEL_DATA.duplicate());
            writeLF(writeCharSequence(out, data));
        }

        static void writeEvent(ByteBuf out, CharSequence event) {
            out.writeBytes(LABEL_EVENT.duplicate());
            writeLF(writeCharSequence(out, event));
        }

        static void writeId(ByteBuf out, Object id) {
            out.writeBytes(LABEL_ID.duplicate());
            writeLF(writeValue(out, id));
        }

    }

    private static final class MessageSseEvent extends AbstractSseEvent {

        private final CharSequence data;

        private MessageSseEvent(CharSequence data) {
            this.data = Objects.requireNonNull(data, "data must not be null");
        }

        @Override
        public void serialize(ByteBuf out) {
            writeData(out, data);
            writeLF(out);
        }

        @Override
        public ByteBuf serialize(ByteBufAllocator allocator) {
            var data = this.data;
            // buffer length = "data: ".length + data.length + "\n".length
            ByteBuf out;
            if (data instanceof AsciiString ascii) {
                var length = 7 + ascii.length();
                out = allocator.buffer(length);
                out.writeBytes(LABEL_DATA.duplicate());
                ByteBufUtil.writeAscii(out, ascii);
            } else {
                var bytes = data.toString().getBytes(StandardCharsets.UTF_8);
                var length = 7 + data.length();
                out = allocator.buffer(length);
                out.writeBytes(LABEL_DATA.duplicate());
                out.writeBytes(bytes);
            }
            writeLF(out);
            writeLF(out);
            return out;
        }

        @Override
        public String toString() {
            return "{data: " + data + "}";
        }
    }

    private static final class EventOnlySseEvent extends AbstractSseEvent {

        private final CharSequence event;

        private EventOnlySseEvent(CharSequence event) {
            this.event = Objects.requireNonNull(event, "event must not be null");
        }

        @Override
        public void serialize(ByteBuf out) {
            writeData(out, event);
            writeLF(out);
        }

        @Override
        public ByteBuf serialize(ByteBufAllocator allocator) {
            var event = this.event;
            // buffer length = "event: ".length + event.length + "\n".length
            ByteBuf out;
            if (event instanceof AsciiString ascii) {
                var length = 8 + ascii.length();
                out = allocator.buffer(length);
                out.writeBytes(LABEL_EVENT.duplicate());
                ByteBufUtil.writeAscii(out, ascii);
            } else {
                var bytes = event.toString().getBytes(StandardCharsets.UTF_8);
                var length = 8 + event.length();
                out = allocator.buffer(length);
                out.writeBytes(LABEL_EVENT.duplicate());
                out.writeBytes(bytes);
            }
            writeLF(out);
            writeLF(out);
            return out;
        }

        @Override
        public String toString() {
            return "{event: " + event + "}";
        }
    }

    private static final class DefaultSseEvent extends AbstractSseEvent {

        private final CharSequence event;
        private final Object id;
        private final CharSequence data;

        private DefaultSseEvent(CharSequence event, Object id, CharSequence data) {
            this.event = event;
            this.id = id;
            this.data = data;
        }

        @Override
        public void serialize(ByteBuf out) {
            var event = this.event;
            if (event != null) {
                writeEvent(out, event);
            }
            var id = this.id;
            if (id != null) {
                writeId(out, id);
            }
            var data = this.data;
            if (data != null) {
                writeData(out, data);
            }
            writeLF(out);
        }

        @Override
        public String toString() {
            return "{event: " + event + ", id: " + id + ", data: " + data + "}";
        }

    }

}
