package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;
import static com.github.fmjsjx.libnetty.resp3.Resp3MessageType.STREAMED_STRING_PART;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * The cached implementation of {@link Resp3StreamedStringPartMessage} for last
 * string part (end of the streamed string).
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedLastStreamedStringPartMessage extends CachedRespMessage implements Resp3StreamedStringPartMessage {

    static final CachedLastStreamedStringPartMessage INSTANCE = new CachedLastStreamedStringPartMessage();

    /**
     * Returns the singleton {@link CachedLastStreamedStringPartMessage} instance.
     * 
     * @return the singleton {@link CachedLastStreamedStringPartMessage} instance
     */
    public static final CachedLastStreamedStringPartMessage getInstance() {
        return INSTANCE;
    }

    private CachedLastStreamedStringPartMessage() {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(STREAMED_STRING_PART.value()).writeByte('0')
                .writeShort(EOL_SHORT));
    }

    @Override
    public CachedLastStreamedStringPartMessage copy() {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage duplicate() {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage retainedDuplicate() {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage replace(ByteBuf content) {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage retain() {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage retain(int increment) {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage touch() {
        return this;
    }

    @Override
    public CachedLastStreamedStringPartMessage touch(Object hint) {
        return this;
    }

    @Override
    public ByteBuf content() {
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public int refCnt() {
        return fullContent.refCnt();
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }

}
