package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.nio.charset.Charset;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.AbstractRespContent;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AsciiString;

/**
 * The default implementation of {@link Resp3VerbatimStringMessage}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultVerbatimStringMessage extends AbstractRespContent<DefaultVerbatimStringMessage>
        implements Resp3VerbatimStringMessage {

    private final AsciiString formatPart;
    private final Format format;

    /**
     * Constructs a new {@link DefaultVerbatimStringMessage} with the specified
     * formatPart and content.
     * 
     * @param format  the format
     * @param content the content
     */
    public DefaultVerbatimStringMessage(Format format, ByteBuf content) {
        this(format.abbr(), format, content);
    }

    /**
     * Constructs a new {@link DefaultVerbatimStringMessage} with the specified
     * formatPart and content.
     * 
     * @param formatPart the format part string
     * @param content    the content
     */
    public DefaultVerbatimStringMessage(String formatPart, ByteBuf content) {
        this(AsciiString.cached(formatPart), content);
    }

    /**
     * Constructs a new {@link DefaultVerbatimStringMessage} with the specified
     * formatPart and content.
     * 
     * @param formatPart the format part string
     * @param content    the content
     */
    public DefaultVerbatimStringMessage(AsciiString formatPart, ByteBuf content) {
        this(formatPart, Format.fromAbbr(formatPart), content);
    }

    private DefaultVerbatimStringMessage(AsciiString formatPart, Format format, ByteBuf content) {
        super(content.asReadOnly());
        this.formatPart = formatPart;
        this.format = format;
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        ByteBuf content = content();
        int length = content.readableBytes() + 4;
        byte[] lengthValue = RespCodecUtil.longToAsciiBytes(length);
        ByteBuf header = RespCodecUtil.buffer(alloc, TYPE_LENGTH + lengthValue.length + EOL_LENGTH + 4);
        header.writeByte(type().value()).writeBytes(lengthValue).writeShort(EOL_SHORT).writeBytes(formatPart.array())
                .writeByte(':');
        out.add(header);
        out.add(content.retain());
        out.add(EOL_BUF.duplicate());
    }

    @Override
    public AsciiString formatPart() {
        return formatPart;
    }

    @Override
    public Format format() {
        return format;
    }

    @Override
    public String textValue(Charset charset) {
        return content().toString(charset);
    }

    @Override
    public DefaultVerbatimStringMessage replace(ByteBuf content) {
        return new DefaultVerbatimStringMessage(formatPart, format, content);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + formatPart + ":" + RespCodecUtil.toString(content()) + "]";
    }

}
