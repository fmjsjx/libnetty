package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A {@code FCGI_STDIN} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiStdin extends AbstractFcgiContent<FcgiStdin> {

    /**
     * Constructs a new {@link FcgiStdin} instance with the specified
     * {@link ByteBuf} content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param content         the {@code ByteBuf} content
     */
    public FcgiStdin(FcgiVersion protocolVersion, int requestId, ByteBuf content) {
        super(protocolVersion, requestId, content);
    }

    /**
     * Constructs a new {@link FcgiStdin} instance with empty content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     */
    public FcgiStdin(FcgiVersion protocolVersion, int requestId) {
        this(protocolVersion, requestId, Unpooled.EMPTY_BUFFER);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.STDIN;
    }

    @Override
    public FcgiStdin replace(ByteBuf content) {
        return new FcgiStdin(protocolVersion, requestId, content);
    }

}
