package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A {@code FCGI_STDOUT} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiStdout extends AbstractFcgiContent<FcgiStdout> {

    /**
     * Constructs a new {@link FcgiStdout} instance with the specified
     * {@link ByteBuf} content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param content         the {@code ByteBuf} content
     */
    public FcgiStdout(FcgiVersion protocolVersion, int requestId, ByteBuf content) {
        super(protocolVersion, requestId, content);
    }

    /**
     * Constructs a new {@link FcgiStdout} instance with empty content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     */
    public FcgiStdout(FcgiVersion protocolVersion, int requestId) {
        this(protocolVersion, requestId, Unpooled.EMPTY_BUFFER);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.STDOUT;
    }

    @Override
    public FcgiStdout replace(ByteBuf content) {
        return new FcgiStdout(protocolVersion, requestId, content);
    }

}
