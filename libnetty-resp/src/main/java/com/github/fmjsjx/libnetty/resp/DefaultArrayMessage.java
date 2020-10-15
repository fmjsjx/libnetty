package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

/**
 * The default implementation of {@link RespArrayMessage}.
 * 
 * @param <E> the type of elements in this array.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultArrayMessage<E extends RespMessage> extends AbstractRespAggregateMessage<E, DefaultArrayMessage<E>>
        implements RespArrayMessage<E> {

    /**
     * The empty array instance (immutable).
     */
    public static final DefaultArrayMessage<? extends RespMessage> EMPTY = new EmptyArrayMessage();

    /**
     * Returns the empty array instance.
     * 
     * @param <E> the type of elements in the array
     * @return the empty array
     * 
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public static final <E extends RespMessage> DefaultArrayMessage<E> empty() {
        return (DefaultArrayMessage<E>) EMPTY;
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the number values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArrayLong(ByteBufAllocator alloc,
            Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return empty();
        }
        List<RespBulkStringMessage> list = values.stream()
                .map(v -> DefaultBulkStringMessage.create(alloc, v.longValue())).collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the number values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArray(ByteBufAllocator alloc,
            long... values) {
        if (values.length == 0) {
            return empty();
        }
        List<RespBulkStringMessage> list = Arrays.stream(values)
                .mapToObj(v -> DefaultBulkStringMessage.create(alloc, v)).collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the number values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArray(ByteBufAllocator alloc,
            int... values) {
        if (values.length == 0) {
            return empty();
        }
        List<RespBulkStringMessage> list = Arrays.stream(values)
                .mapToObj(v -> DefaultBulkStringMessage.create(alloc, v)).collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code US-ASCII} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArrayAscii(ByteBufAllocator alloc,
            Collection<? extends CharSequence> values) {
        if (values.isEmpty()) {
            return empty();
        }
        List<RespBulkStringMessage> list = values.stream().map(v -> DefaultBulkStringMessage.createAscii(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code US-ASCII} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArrayAscii(ByteBufAllocator alloc,
            CharSequence... values) {
        if (values.length == 0) {
            return empty();
        }
        List<RespBulkStringMessage> list = Arrays.stream(values)
                .map(v -> DefaultBulkStringMessage.createAscii(alloc, v)).collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code UTF-8} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArrayUtf8(ByteBufAllocator alloc,
            Collection<? extends CharSequence> values) {
        if (values.isEmpty()) {
            return empty();
        }
        List<RespBulkStringMessage> list = values.stream().map(v -> DefaultBulkStringMessage.createUtf8(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code UTF-8} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage<RespBulkStringMessage> bulkStringArrayUtf8(ByteBufAllocator alloc,
            CharSequence... values) {
        if (values.length == 0) {
            return empty();
        }
        List<RespBulkStringMessage> list = Arrays.stream(values).map(v -> DefaultBulkStringMessage.createUtf8(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage<>(list);
    }

    private final List<E> values;

    /**
     * Constructs a {@link DefaultArrayMessage} containing the values of the
     * specified array.
     * 
     * @param values a {@link RespMessage} array
     */
    @SafeVarargs
    public DefaultArrayMessage(E... values) {
        this(Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * Constructs a {@link DefaultArrayMessage} containing the specified list
     * {@code values}.
     * 
     * @param values a {@link RespMessage} list
     */
    DefaultArrayMessage(List<E> values) {
        this.values = Objects.requireNonNull(values, "values must not be null");
    }

    /**
     * Constructs a {@link DefaultArrayMessage} containing the values of the
     * specified collection.
     * 
     * @param values a {@link RespMessage} collection
     */
    public DefaultArrayMessage(Collection<E> values) {
        this(new ArrayList<>(values));
    }

    /**
     * Constructs a {@link DefaultArrayMessage}.
     */
    public DefaultArrayMessage() {
        this(new ArrayList<>());
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        byte[] sizeBytes = RespCodecUtil.longToAsciiBytes(size());
        ByteBuf header = alloc.buffer(TYPE_LENGTH + sizeBytes.length + EOL_LENGTH).writeByte(type().value())
                .writeBytes(sizeBytes).writeShort(EOL_SHORT);
        out.add(header); // array header
    }

    @Override
    protected void encodeValue(ByteBufAllocator alloc, E value, List<Object> out) throws Exception {
        value.encode(alloc, out);
    }

    @Override
    public DefaultArrayMessage<E> touch(Object hint) {
        for (RespMessage value : values) {
            ReferenceCountUtil.touch(value, hint);
        }
        return this;
    }

    @Override
    protected void deallocate() {
        for (RespMessage value : values) {
            ReferenceCountUtil.release(value);
        }
    }

    @Override
    public List<E> values() {
        return values;
    }

    /**
     * Appends the specified value to the end of this array.
     * 
     * @param value the value
     * @return {@code true}
     */
    public boolean add(E value) {
        return values.add(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(" + size() + ")" + values() + "]";
    }

    private static final class EmptyArrayMessage extends DefaultArrayMessage<RespMessage> {

        private static final ByteBuf sizeBuf = Unpooled.unreleasableBuffer(
                RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(RespMessageType.ARRAY.value())
                        .writeBytes(RespCodecUtil.longToAsciiBytes(0)).writeShort(EOL_SHORT).asReadOnly());

        private EmptyArrayMessage() {
            super(Collections.emptyList());
        }

        @Override
        public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
            out.add(sizeBuf.duplicate());
        }

        @Override
        public int size() {
            return 0;
        }

    }

}
