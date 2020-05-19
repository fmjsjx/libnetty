package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;

import java.nio.charset.Charset;
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
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.AbstractReferenceCounted;

/**
 * The default implementation of {@link RespArrayMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultArrayMessage extends AbstractReferenceCounted implements RespArrayMessage {

    /**
     * The empty array instance (immutable).
     */
    public static final DefaultArrayMessage EMPTY = new EmptyArrayMessage();

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the number values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArrayLong(ByteBufAllocator alloc,
            Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        List<RespMessage> list = values.stream().map(v -> DefaultBulkStringMessage.create(alloc, v.longValue()))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the number values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArray(ByteBufAllocator alloc, long... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        List<RespMessage> list = Arrays.stream(values).mapToObj(v -> DefaultBulkStringMessage.create(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the number values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArray(ByteBufAllocator alloc, int... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        List<RespMessage> list = Arrays.stream(values).mapToObj(v -> DefaultBulkStringMessage.create(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code US-ASCII} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArrayAscii(ByteBufAllocator alloc,
            Collection<? extends CharSequence> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        List<RespMessage> list = values.stream().map(v -> DefaultBulkStringMessage.createAscii(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code US-ASCII} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArrayAscii(ByteBufAllocator alloc, CharSequence... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        List<RespMessage> list = Arrays.stream(values).map(v -> DefaultBulkStringMessage.createAscii(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code UTF-8} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArrayUtf8(ByteBufAllocator alloc,
            Collection<? extends CharSequence> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        List<RespMessage> list = values.stream().map(v -> DefaultBulkStringMessage.createUtf8(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc  the allocator to allocate {@link ByteBuf}s
     * @param values the string values with {@code UTF-8} character set
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArrayUtf8(ByteBufAllocator alloc, CharSequence... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        List<RespMessage> list = Arrays.stream(values).map(v -> DefaultBulkStringMessage.createUtf8(alloc, v))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc   the allocator to allocate {@link ByteBuf}s
     * @param charset the {@link Charset} of the string values
     * @param values  the string values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArray(ByteBufAllocator alloc, Charset charset,
            CharSequence... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        List<RespMessage> list = Arrays.stream(values).map(v -> DefaultBulkStringMessage.create(alloc, v, charset))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    /**
     * Returns a new {@link DefaultArrayMessage} with the given values.
     * 
     * @param alloc   the allocator to allocate {@link ByteBuf}s
     * @param charset the {@link Charset} of the string values
     * @param values  the string values
     * @return a {@code DefaultArrayMessage}
     */
    public static final DefaultArrayMessage bulkStringArray(ByteBufAllocator alloc, Charset charset,
            Collection<? extends CharSequence> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        List<RespMessage> list = values.stream().map(v -> DefaultBulkStringMessage.create(alloc, v, charset))
                .collect(Collectors.toList());
        return new DefaultArrayMessage(list);
    }

    private final List<? extends RespMessage> values;

    /**
     * Constructs a {@link DefaultArrayMessage} containing the values of the
     * specified array.
     * 
     * @param values a {@link RespMessage} array
     */
    public DefaultArrayMessage(RespMessage... values) {
        this(Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * Constructs a {@link DefaultArrayMessage} containing the specified list
     * {@code values}.
     * 
     * @param values a {@link RespMessage} list
     */
    DefaultArrayMessage(List<? extends RespMessage> values) {
        this.values = Objects.requireNonNull(values, "values must not be null");
    }

    /**
     * Constructs a {@link DefaultArrayMessage} containing the values of the
     * specified collection.
     * 
     * @param values a {@link RespMessage} collection
     */
    public DefaultArrayMessage(Collection<? extends RespMessage> values) {
        this(new ArrayList<>(values));
    }

    /**
     * Constructs a {@link DefaultArrayMessage}.
     */
    public DefaultArrayMessage() {
        this(new ArrayList<>());
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.ARRAY;
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        byte[] sizeBytes = RespCodecUtil.longToAsciiBytes(size());
        ByteBuf sizeBuf = alloc.buffer(sizeBytes.length + EOL_LENGTH).writeBytes(sizeBytes).writeShort(EOL_SHORT);
        out.add(type().content());
        out.add(sizeBuf); // size
        for (RespMessage value : values) {
            value.encode(alloc, out);
        }
    }

    @Override
    public DefaultArrayMessage touch(Object hint) {
        values.forEach(RespMessage::touch);
        return this;
    }

    @Override
    protected void deallocate() {
        values.forEach(RespMessage::release);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public List<? extends RespMessage> values() {
        return values;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + "(" + size() + ")" + values() + "]";
    }

    private static final class EmptyArrayMessage extends DefaultArrayMessage {

        private static final ByteBuf sizeBuf = Unpooled
                .unreleasableBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(1 + EOL_LENGTH, 1 + EOL_LENGTH)
                        .writeBytes(RespCodecUtil.longToAsciiBytes(0)).writeShort(EOL_SHORT).asReadOnly());

        private EmptyArrayMessage() {
            super(Collections.emptyList());
        }

        @Override
        public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
            out.add(type().content());
            out.add(sizeBuf.duplicate());
        }

        @Override
        public int size() {
            return 0;
        }

    }

}
