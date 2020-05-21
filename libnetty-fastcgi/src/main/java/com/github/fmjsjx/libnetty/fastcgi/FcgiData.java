package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A {@code FCGI_DATA} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiData extends AbstractFcgiContent<FcgiData> {

    /**
     * Constructs a new {@link FcgiData} instance with the specified {@link ByteBuf}
     * content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param content         the {@code ByteBuf} content
     */
    public FcgiData(FcgiVersion protocolVersion, int requestId, ByteBuf content) {
        super(protocolVersion, requestId, content);
    }

    /**
     * Constructs a new {@link FcgiData} instance with empty content.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     */
    public FcgiData(FcgiVersion protocolVersion, int requestId) {
        this(protocolVersion, requestId, Unpooled.EMPTY_BUFFER);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.DATA;
    }

    @Override
    public FcgiData replace(ByteBuf content) {
        return new FcgiData(protocolVersion, requestId, content);
    }

}
