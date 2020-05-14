package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCounted;

/**
 * An interface that defines an RESP message, providing common properties and
 * methods for RESP messages.
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
public interface RespMessage extends RespObject, ReferenceCounted {

    /**
     * Returns the type of this {@link RespMessage}.
     * 
     * @return a {@link RespMessageType}
     */
    RespMessageType type();

    /**
     * Encode this message.
     * 
     * @param alloc the {@link ByteBufAllocator} which will be used to allocate
     *              {@link ByteBuf}s
     * @param out   the {@link List} into which the encoded msg should be added
     * @throws Exception is thrown if an error occurs
     */
    void encode(ByteBufAllocator alloc, List<Object> out) throws Exception;

}
