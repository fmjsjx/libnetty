package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_BUF;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.util.List;

import com.github.fmjsjx.libnetty.resp.AbstractRespContent;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * The default implementation of {@link Resp3StreamedStringPartMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultStreamedStringPartMessage extends AbstractRespContent<DefaultStreamedStringPartMessage>
        implements Resp3StreamedStringPartMessage {

    private static final IllegalArgumentException CONTENT_MUST_NOT_BE_EMPTY = new IllegalArgumentException(
            "content must not be empty in streamed string part");

    /**
     * Creates a new {@link DefaultStreamedStringPartMessage} witch wraps the
     * specified content.
     * 
     * @param content the content
     * @return a {@code DefaultStreamedStringPartMessage}
     */
    public static final DefaultStreamedStringPartMessage wrap(ByteBuf content) {
        if (!content.isReadable()) {
            throw CONTENT_MUST_NOT_BE_EMPTY;
        }
        return new DefaultStreamedStringPartMessage(content.asReadOnly());
    }

    private DefaultStreamedStringPartMessage(ByteBuf content) {
        super(content);
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        ByteBuf content = content();
        int length = content.readableBytes();
        byte[] lengthValue = RespCodecUtil.longToAsciiBytes(length);
        ByteBuf header = alloc.buffer(TYPE_LENGTH + lengthValue.length + EOL_LENGTH);
        header.writeByte(type().value()).writeBytes(lengthValue).writeShort(EOL_SHORT);
        out.add(header);
        out.add(content.retain());
        out.add(EOL_BUF.duplicate());
    }

    @Override
    public DefaultStreamedStringPartMessage replace(ByteBuf content) {
        return new DefaultStreamedStringPartMessage(content);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + RespCodecUtil.toString(content()) + "]";
    }

}
