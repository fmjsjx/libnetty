package com.github.fmjsjx.libnetty.resp;

import java.util.List;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCounted;

public interface RespMessage extends RespObject, ReferenceCounted {

    RespMessageType type();

    void encode(ByteBufAllocator alloc, List<Object> out) throws Exception;

}
