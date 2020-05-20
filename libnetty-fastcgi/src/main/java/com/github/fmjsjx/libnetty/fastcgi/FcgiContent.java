package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.ByteBufHolder;

/**
 * An interface defines a FastCGI content record. Combines the
 * {@link FcgiRecord} and the {@link ByteBufHolder}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface FcgiContent extends FcgiRecord, ByteBufHolder {

    @Override
    default int contentLength() {
        return content().readableBytes();
    }

    @Override
    default int paddingLength() {
        return FcgiCodecUtil.calculatePaddingLength(contentLength());
    }

}
