package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * The abstract implementation of {@link RespMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public abstract class AbstractSimpleRespMessage implements RespMessage {

    protected abstract byte[] encodedValue() throws Exception;

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        byte[] encodedValue = encodedValue();
        ByteBuf content = alloc.buffer(TYPE_LENGTH + encodedValue.length + EOL_LENGTH);
        content.writeByte(type().value()).writeBytes(encodedValue).writeShort(EOL_SHORT);
        out.add(content);
    }

}
