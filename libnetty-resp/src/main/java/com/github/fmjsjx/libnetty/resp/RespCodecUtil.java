package com.github.fmjsjx.libnetty.resp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.PlatformDependent;

/**
 * In-package accessible utility class for RESP CODEC.
 *
 * @since 1.0
 *
 * @author MJ Fang
 */
class RespCodecUtil {

    static byte[] longToAsciiBytes(long value) {
        if (value > -128 && value < 127) { // cached
            return LongCache.asciiBytesCache[(int) value + 128];
        }
        return encodeLongToAscii(value);
    }

    static byte[] doubleToAsciiBytes(double value) {
        return Double.toString(value).getBytes(CharsetUtil.US_ASCII);
    }

    private static byte[] encodeLongToAscii(long value) {
        return Long.toString(value).getBytes(CharsetUtil.US_ASCII);
    }

    static short makeShort(char first, char second) {
        return PlatformDependent.BIG_ENDIAN_NATIVE_ORDER ? (short) ((second << 8) | first)
                : (short) ((first << 8) | second);
    }

    static byte[] shortToBytes(short value) {
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

    static ByteBuf writeAsciiFixed(ByteBufAllocator alloc, AsciiString ascii) {
        if (ascii.length() == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        return alloc.buffer(ascii.length(), ascii.length()).writeBytes(ascii.array());
    }

    static ByteBuf writeStringFixed(ByteBufAllocator alloc, String string, Charset charset) {
        byte[] b = string.getBytes(charset);
        if (b.length == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        return alloc.buffer(b.length, b.length).writeBytes(b);
    }

    static String toString(ByteBuf content) {
        return content == null ? null : content.toString(CharsetUtil.UTF_8);
    }

    static String toString(ByteBuf content, Charset charset) {
        return content == null ? null : content.toString(charset);
    }

    static int decodeInt(ByteBuf content) {
        int begin = content.readerIndex();
        int end = content.writerIndex();
        boolean negative = content.getByte(begin) == '-' ? true : false;
        if (negative) {
            begin++;
        }
        ToPositiveIntProcessor numberProcessor = new ToPositiveIntProcessor();
        content.forEachByte(begin, end - begin, numberProcessor);
        return numberProcessor.value;
    }

    static long decodeLong(ByteBuf content) {
        return decodeLong(content, new ToPositiveLongProcessor());
    }
    
    static long decodeLong(ByteBuf content, ToPositiveLongProcessor numberProcessor) {
        int begin = content.readerIndex();
        int length = content.readableBytes();
        if (length == 0) {
            throw NaN;
        }
        boolean negative = content.getByte(begin) == '-' ? true : false;
        if (negative) {
            begin++;
            length--;
        }
        if (length == 0) {
            throw NaN;
        }
        content.forEachByte(begin, length, numberProcessor);
        return numberProcessor.value;
    }

    static final NumberFormatException NaN = new NumberFormatException("value is not a number");

    private RespCodecUtil() {
    }

    static final class ToPositiveIntProcessor implements ByteProcessor {

        private static final NumberFormatException int32Overflow = new NumberFormatException(
                "number overflow for 32-bit integer");

        private int value;

        @Override
        public boolean process(byte value) throws Exception {
            int old = this.value;
            if (value >= '0' && value <= '9') {
                this.value = old * 10 + (value - '0');
                if (this.value < 0) {
                    throw int32Overflow;
                }
                return true;
            } else {
                throw NaN;
            }
        }

        int value() {
            return value;
        }

        void reset() {
            value = 0;
        }

    }

    static final class ToPositiveLongProcessor implements ByteProcessor {

        private static final NumberFormatException int64Overflow = new NumberFormatException(
                "number overflow for 64-bit integer");

        private long value;

        @Override
        public boolean process(byte value) throws Exception {
            long old = this.value;
            if (value >= '0' && value <= '9') {
                this.value = old * 10 + (value - '0');
                if (this.value < 0) {
                    throw int64Overflow;
                }
                return true;
            } else {
                throw NaN;
            }
        }

        long value() {
            return value;
        }

        void reset() {
            value = 0L;
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
