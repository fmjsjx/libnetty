package com.github.fmjsjx.libnetty.resp;

import io.netty.buffer.ByteBuf;

/**
 * The abstract implementation of the {@link RespMessage}, combines the
 * {@link RespMessage} and {@link RespContent}.
 * 
 * @param <Self> the type of the Super Class
 *
 * @since 1.0
 * 
 * @author MJ Fang
 */
public abstract class AbstractContentRespMessage<Self extends RespContent> extends AbstractRespContent<Self>
        implements RespMessage {

    protected AbstractContentRespMessage(ByteBuf content) {
        super(content);
    }

    protected AbstractContentRespMessage() {
        super();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + RespCodecUtil.toString(data) + "]";
    }

}
