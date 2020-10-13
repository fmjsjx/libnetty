package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * The abstract implementation of the {@link CachedRespMessage}.
 *
 * @param <Self> the type of the Super Class
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public abstract class AbstractCachedRespMessage<Self extends CachedRespMessage> extends AbstractRespContent<Self>
        implements CachedRespMessage {

    protected static final ByteBufAllocator ALLOC = UnpooledByteBufAllocator.DEFAULT;

    protected static final ByteBuf fixedBuffer(int contentLength) {
        return ALLOC.buffer(TYPE_LENGTH + contentLength + EOL_LENGTH, TYPE_LENGTH + contentLength + EOL_LENGTH);
    }

    protected final ByteBuf fullContent;

    protected AbstractCachedRespMessage(ByteBuf content, ByteBuf fullContent) {
        super(Unpooled.unreleasableBuffer(content));
        this.fullContent = Unpooled.unreleasableBuffer(fullContent);
    }

    @Override
    public ByteBuf content() {
        return data.duplicate();
    }

    protected ByteBuf fullContent() {
        return fullContent.duplicate();
    }

    @Override
    public void encode(List<Object> out) throws Exception {
        out.add(fullContent());
    }

}
