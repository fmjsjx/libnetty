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
        out.writeByte(record.protocolVersion().version());
        out.writeByte(record.type().type());
        out.writeShort(record.requestId());
        out.writeShort(record.contentLength());
        out.writeByte(record.paddingLength());
        out.writeZero(1);
    }
    
    static final int getVersion(ByteBuf buf) {
        return buf.getByte(0);
    }
    
    static final int getType(ByteBuf buf) {
        return buf.getByte(1);
    }
    
    static final int getRequestId(ByteBuf buf) {
        return buf.getUnsignedShort(2);
    }
    
    static final int getContentLength(ByteBuf buf) {
        return buf.getUnsignedShort(3);
    }
    
    static final int getPaddingLength(ByteBuf buf) {
        return buf.getUnsignedByte(5);
    }
    
    static final int getDataLength(ByteBuf buf) {
        return getContentLength(buf) + getPaddingLength(buf);
    }

    private FcgiCodecUtil() {
    }

}
