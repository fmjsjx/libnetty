package com.github.fmjsjx.libnetty.resp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.PlatformDependent;

/**
 * Utility class for RESP CODEC.
 *
 * @since 1.0
 *
 * @author MJ Fang
 */
public class RespCodecUtil {

    /**
     * Convert long value to ASCII bytes.
     * 
     * @param value the value
     * @return ASCII bytes
     */
    public static final byte[] longToAsciiBytes(long value) {
        if (value > -128 && value < 127) { // cached
            return LongCache.asciiBytesCache[(int) value + 128];
        }
        return encodeLongToAscii(value);
    }

    /**
     * Convert double value to ASCII bytes.
     * 
     * @param value the value
     * @return ASCII bytes
     */
    public static final byte[] doubleToAsciiBytes(double value) {
        return Double.toString(value).getBytes(CharsetUtil.US_ASCII);
    }

    private static final byte[] encodeLongToAscii(long value) {
        return Long.toString(value).getBytes(CharsetUtil.US_ASCII);
    }

    /**
     * Convert to short value.
     * 
     * @param first  the first ASCII char
     * @param second the second ASCII char
     * @return the converted short value
     */
    public static final short makeShort(char first, char second) {
        return PlatformDependent.BIG_ENDIAN_NATIVE_ORDER ? (short) ((second << 8) | first)
                : (short) ((first << 8) | second);
    }

    /**
     * Convert short value to ASCII bytes.
     * 
     * @param value the value
     * @return ASCII bytes
     */
    public static final byte[] shortToBytes(short value) {
        byte[] bytes = new byte[2];
        if (PlatformDependent.BIG_ENDIAN_NATIVE_ORDER) {
            bytes[1] = (byte) ((value >> 8) & 0xff);
            bytes[0] = (byte) (value & 0xff);
        } else {
            bytes[0] = (byte) ((value >> 8) & 0xff);
            bytes[1] = (byte) (value & 0xff);
        }
        return bytes;
    }

    /**
     * Writes ASCII string to a fixed {@link ByteBuf}.
     * 
     * @param alloc the allocator allocates {@link ByteBuf}s
     * @param ascii the ASCII string
     * @return a {@code ByteBuf}
     */
    public static final ByteBuf writeAsciiFixed(ByteBufAllocator alloc, AsciiString ascii) {
        if (ascii.isEmpty()) {
            return Unpooled.EMPTY_BUFFER;
        }
        return alloc.buffer(ascii.length(), ascii.length()).writeBytes(ascii.array());
    }

    /**
     * Writes string to a fixed {@link ByteBuf}.
     * 
     * @param alloc   the allocator allocates {@link ByteBuf}s
     * @param string  the string
     * @param charset the character-set of the string
     * @return a {@code ByteBuf}
     */
    public static final ByteBuf writeStringFixed(ByteBufAllocator alloc, String string, Charset charset) {
        byte[] b = string.getBytes(charset);
        if (b.length == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        return alloc.buffer(b.length, b.length).writeBytes(b);
    }

    /**
     * Safe toString() for {@link ByteBuf}.
     * 
     * @param content the content
     * @return toString() result
     */
    public static final String toString(ByteBuf content) {
        return toString(content, CharsetUtil.UTF_8);
    }

    /**
     * Safe toString() for {@link ByteBuf}.
     * 
     * @param content the content
     * @param charset the character-set of the content
     * @return toString() result
     */
    public static final String toString(ByteBuf content, Charset charset) {
        return content == null ? null : content.toString(charset);
    }

    /**
     * Decode an int value from the specified {@link ByteBuf}.
     * 
     * @param content the content
     * @return the decoded int value
     */
    public static final int decodeInt(ByteBuf content) {
        int begin = content.readerIndex();
        int end = content.writerIndex();
        if (end == begin) {
            throw NaN;
        }
        boolean negative = content.getByte(begin) == '-';
        if (negative) {
            begin++;
        }
        int length = end - begin;
        if (length == 0) {
            throw NaN;
        }
        ToPositiveIntProcessor numberProcessor = ThreadLocalToPositiveIntProcessor.INSTANCE.get();
        numberProcessor.reset();
        content.forEachByte(begin, length, numberProcessor);
        return numberProcessor.value;
    }

    /**
     * Decode an unsigned int value from the specified {@link ByteBuf}.
     * 
     * @param content the content
     * @return the decoded unsigned int value
     * 
     * @since 1.1
     */
    public static final int decodeUnsignedInt(ByteBuf content) {
        if (!content.isReadable()) {
            throw NaN;
        }
        ToPositiveIntProcessor numberProcessor = ThreadLocalToPositiveIntProcessor.INSTANCE.get();
        numberProcessor.reset();
        content.forEachByte(numberProcessor);
        return numberProcessor.value;
    }

    /**
     * Decode a long value from the specified {@link ByteBuf}.
     * 
     * @param content the content
     * @return the decoded long value
     */
    public static final long decodeLong(ByteBuf content) {
        int begin = content.readerIndex();
        int end = content.writerIndex();
        if (begin == end) {
            throw NaN;
        }
        boolean negative = content.getByte(begin) == '-';
        if (negative) {
            begin++;
        }
        int length = end - begin;
        if (length == 0) {
            throw NaN;
        }
        ToPositiveLongProcessor numberProcessor = ThreadLocalToPositiveLongProcessor.INSTANCE.get();
        numberProcessor.reset();
        content.forEachByte(begin, length, numberProcessor);
        return numberProcessor.value;
    }

    /**
     * Decode an unsigned long value from the specified {@link ByteBuf}.
     * 
     * @param content the content
     * @return the decoded unsigned long value
     * 
     * @since 1.1
     */
    public static final long decodeUnsignedLong(ByteBuf content) {
        if (!content.isReadable()) {
            throw NaN;
        }
        ToPositiveLongProcessor numberProcessor = toPositiveLongProcessor();
        content.forEachByte(numberProcessor);
        return numberProcessor.value;
    }

    /**
     * Returns a {@code thread-local} {@link ToPositiveIntProcessor} instance that
     * already be reset.
     * 
     * @return a {@code thread-local} {@link ToPositiveIntProcessor} instance
     * 
     * @since 1.1
     */
    public static final ToPositiveIntProcessor toPositiveIntProcessor() {
        ToPositiveIntProcessor numberProcessor = ThreadLocalToPositiveIntProcessor.INSTANCE.get();
        numberProcessor.reset();
        return numberProcessor;
    }

    /**
     * Returns a {@code thread-local} {@link ToPositiveLongProcessor} instance that
     * already be reset.
     * 
     * @return a {@code thread-local} {@link ToPositiveLongProcessor} instance
     * 
     * @since 1.1
     */
    public static final ToPositiveLongProcessor toPositiveLongProcessor() {
        ToPositiveLongProcessor numberProcessor = ThreadLocalToPositiveLongProcessor.INSTANCE.get();
        numberProcessor.reset();
        return numberProcessor;
    }

    /**
     * Decode a long value from the specified {@link ByteBuf}.
     * 
     * @param content         the content
     * @param numberProcessor a processor to parse numbers
     * @return decoded long value
     * 
     * @deprecated please always use {@link #decodeLong(ByteBuf)}
     */
    @Deprecated
    public static final long decodeLong(ByteBuf content, ToPositiveLongProcessor numberProcessor) {
        return decodeLong(content);
    }

    /**
     * Not a number exception.
     */
    public static final NumberFormatException NaN = new NumberFormatException("value is not a number");

    private static final ByteBufAllocator ALLOC = UnpooledByteBufAllocator.DEFAULT;

    /**
     * Allocate a {@link ByteBuf} with the given capacity.
     * 
     * @param alloc    the {@link ByteBufAllocator} that allocates {@link ByteBuf}s
     * @param capacity the capacity (both initial and maximal)
     * @return a {@code ByteBuf}
     * 
     * @since 1.1
     */
    public static final ByteBuf buffer(ByteBufAllocator alloc, int capacity) {
        return alloc.buffer(capacity, capacity);
    }

    /**
     * Allocate a {@link ByteBuf} with the given capacity.
     * 
     * @param capacity the capacity (both initial and maximal)
     * @return a {@code ByteBuf}
     * 
     * @since 1.1
     */
    public static final ByteBuf buffer(int capacity) {
        return buffer(ALLOC, capacity);
    }

    private RespCodecUtil() {
    }

    /**
     * A {@link ByteProcessor} to parse positive int value.
     * 
     * @since 1.0
     *
     * @author MJ Fang
     */
    public static final class ToPositiveIntProcessor implements ByteProcessor {

        private static final NumberFormatException int32Overflow = new NumberFormatException(
                "number overflow for 32-bit integer");

        private int value;

        /**
         * Constructs a new {@link ToPositiveIntProcessor} instance.
         */
        public ToPositiveIntProcessor() {
        }

        @Override
        public boolean process(byte value) {
            int num = this.value;
            if (value >= '0' && value <= '9') {
                this.value = num = num * 10 + (value - '0');
                if (num < 0) {
                    throw int32Overflow;
                }
                return true;
            } else {
                throw NaN;
            }
        }

        /**
         * Returns the parsed int value.
         * 
         * @return the parsed int value
         */
        public int value() {
            return value;
        }

        /**
         * Reset this processor.
         */
        public void reset() {
            value = 0;
        }

    }

    private static final class ThreadLocalToPositiveIntProcessor extends FastThreadLocal<ToPositiveIntProcessor> {

        private static final ThreadLocalToPositiveIntProcessor INSTANCE = new ThreadLocalToPositiveIntProcessor();

        @Override
        protected ToPositiveIntProcessor initialValue() {
            return new ToPositiveIntProcessor();
        }
    }

    /**
     * A {@link ByteProcessor} to parse positive long value.
     * 
     * @since 1.0
     *
     * @author MJ Fang
     */
    public static final class ToPositiveLongProcessor implements ByteProcessor {

        private static final NumberFormatException int64Overflow = new NumberFormatException(
                "number overflow for 64-bit integer");

        private long value;

        /**
         * Constructs a new {@link ToPositiveLongProcessor} instance.
         */
        public ToPositiveLongProcessor() {
        }

        @Override
        public boolean process(byte value) {
            long num = this.value;
            if (value >= '0' && value <= '9') {
                this.value = num = num * 10 + (value - '0');
                if (num < 0) {
                    throw int64Overflow;
                }
                return true;
            } else {
                throw NaN;
            }
        }

        /**
         * Returns the parsed long value.
         * 
         * @return the parsed long value
         */
        public long value() {
            return value;
        }

        /**
         * Reset this processor.
         */
        public void reset() {
            value = 0L;
        }

    }

    private static final class ThreadLocalToPositiveLongProcessor extends FastThreadLocal<ToPositiveLongProcessor> {

        private static final ThreadLocalToPositiveLongProcessor INSTANCE = new ThreadLocalToPositiveLongProcessor();

        @Override
        protected ToPositiveLongProcessor initialValue() {
            return new ToPositiveLongProcessor();
        }

    }

    private static final class LongCache {

        private static final byte[][] asciiBytesCache = new byte[256][];

        static {
            for (int i = 0; i < asciiBytesCache.length; i++) {
                int value = i - 128;
                asciiBytesCache[i] = encodeLongToAscii(value);
            }
        }

    }

}
