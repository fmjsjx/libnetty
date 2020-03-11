package com.github.fmjsjx.libnetty.resp;

import io.netty.buffer.ByteBuf;

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
        return getClass().getSimpleName() + "[" + type() + RespCodecUtil.toString(content) + "]";
    }

}
