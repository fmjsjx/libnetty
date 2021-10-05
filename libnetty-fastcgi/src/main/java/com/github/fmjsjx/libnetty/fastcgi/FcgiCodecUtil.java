package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.ByteBuf;

/**
 * In-package accessible encode/decode utility of FastCGI.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
class FcgiCodecUtil {

    static final void encodeRecordHeader(FcgiRecord record, ByteBuf out) {
        encodeRecordHeaderWithoutLengths(record, out);
        encodeRecordHeaderLengths(record.contentLength(), record.paddingLength(), out);
    }

    static void encodeRecordHeaderWithoutLengths(FcgiRecord record, ByteBuf out) {
        out.writeByte(record.protocolVersion().version());
        out.writeByte(record.type().type());
        out.writeShort(record.requestId());
    }

    static void encodeRecordHeaderLengths(int contentLength, int paddingLength, ByteBuf out) {
        out.writeShort(contentLength);
        out.writeByte(paddingLength);
        out.writeZero(1);
    }

    static void encodeRecordHeaderLengths(int contentLength, ByteBuf out) {
        encodeRecordHeaderLengths(contentLength, calculatePaddingLength(contentLength), out);
    }

    static final int getVersion(ByteBuf buf) {
        return buf.getByte(buf.readerIndex());
    }

    static final int getType(ByteBuf buf) {
        return buf.getByte(buf.readerIndex() + 1);
    }

    static final int getRequestId(ByteBuf buf) {
        return buf.getUnsignedShort(buf.readerIndex() + 2);
    }

    static final int getContentLength(ByteBuf buf) {
        return buf.getUnsignedShort(buf.readerIndex() + 4);
    }

    static final int getPaddingLength(ByteBuf buf) {
        return buf.getUnsignedByte(buf.readerIndex() + 6);
    }

    static final int getDataLength(ByteBuf buf) {
        return getContentLength(buf) + getPaddingLength(buf);
    }

    static final int calculatePaddingLength(int contentLength) {
        int r8 = contentLength % 8;
        return r8 != 0 ? 8 - r8 : 0;
    }

    static final int calculateNameValuePairLength(byte[] name, byte[] value) {
        return calculateVariableLengthSize(name.length) + calculateVariableLengthSize(value.length) + name.length
                + value.length;
    }

    static final int calculateVariableLengthSize(int length) {
        return length < (1 << 7) ? 1 : 4;
    }

    static final void encodeNameValuePair(byte[] name, byte[] value, ByteBuf out) {
        encodeVariableLength(name.length, out);
        encodeVariableLength(value.length, out);
        out.writeBytes(name);
        out.writeBytes(value);
    }

    static final void encodeVariableLength(int length, ByteBuf out) {
        if (length < (1 << 7)) {
            out.writeByte(length);
        } else {
            out.writeInt(length | (1 << 31));
        }
    }

    static final int decodeVariableLength(ByteBuf in) {
        int length = in.getUnsignedByte(in.readerIndex());
        if ((length & (1 << 7)) == 0) {
            in.skipBytes(1);
            return length;
        } else {
            return in.readInt() & ((1 << 31) - 1);
        }
    }

    private FcgiCodecUtil() {
    }

}
