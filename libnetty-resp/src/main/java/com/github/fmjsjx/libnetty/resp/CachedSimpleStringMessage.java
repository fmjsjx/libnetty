package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class CachedSimpleStringMessage extends AbstractCachedRespMessage<CachedSimpleStringMessage>
        implements RespSimpleStringMessage {

    public static final CachedSimpleStringMessage createAscii(CharSequence message) {
        return create(message, CharsetUtil.US_ASCII);
    }

    public static final CachedSimpleStringMessage createUtf8(CharSequence message) {
        return create(message, CharsetUtil.UTF_8);
    }

    public static final CachedSimpleStringMessage create(CharSequence message, Charset charset) {
        String value = message.toString();
        byte[] bytes = value.getBytes(charset);
        int length = bytes.length;
        ByteBuf fullContent = fixedBuffer(length).writeBytes(RespMessageType.SIMPLE_STRING.content()).writeBytes(bytes)
                .writeShort(EOL_SHORT).asReadOnly();
        ByteBuf content = fullContent.slice(fullContent.readerIndex() + TYPE_LENGTH, length);
        return new CachedSimpleStringMessage(content, fullContent, value, charset);
    }

    private final String value;
    private final Charset charset;

    private CachedSimpleStringMessage(ByteBuf content, ByteBuf fullContent, String value, Charset charset) {
        super(content, fullContent);
        this.value = value;
        this.charset = charset;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.SIMPLE_STRING;
    }

    @Override
    public CachedSimpleStringMessage replace(ByteBuf content) {
        return new CachedSimpleStringMessage(content, fullContent, value, charset);
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + value + "]";
    }

}
