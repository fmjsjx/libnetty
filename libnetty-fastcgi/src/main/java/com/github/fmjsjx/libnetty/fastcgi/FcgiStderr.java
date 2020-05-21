package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A {@code FCGI_STDERR} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiStderr extends AbstractFcgiContent<FcgiStderr> {

    /**
     * Constructs a new {@link FcgiStderr} instance with the specified
     * {@link ByteBuf} content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param content         the {@code ByteBuf} content
     */
    public FcgiStderr(FcgiVersion protocolVersion, int requestId, ByteBuf content) {
        super(protocolVersion, requestId, content);
    }

    /**
     * Constructs a new {@link FcgiStderr} instance with empty content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     */
    public FcgiStderr(FcgiVersion protocolVersion, int requestId) {
        this(protocolVersion, requestId, Unpooled.EMPTY_BUFFER);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.STDERR;
    }

    @Override
    public FcgiStderr replace(ByteBuf content) {
        return new FcgiStderr(protocolVersion, requestId, content);
    }

}
