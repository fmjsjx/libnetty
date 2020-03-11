package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class DefaultErrorMessage extends AbstractContentRespMessage<DefaultErrorMessage> implements RespErrorMessage {

    public static final DefaultErrorMessage createErrAscii(ByteBufAllocator alloc, CharSequence message) {
        return createAscii(alloc, ERR, message);
    }

    public static final DefaultErrorMessage createErrUtf8(ByteBufAllocator alloc, CharSequence message) {
        return createUtf8(alloc, ERR, message);
    }

    public static final DefaultErrorMessage createAscii(ByteBufAllocator alloc, CharSequence code,
            CharSequence message) {
        String text = code + " " + message;
        return new DefaultErrorMessage(ByteBufUtil.writeAscii(alloc, text), AsciiString.of(code), message.toString(),
                CharsetUtil.US_ASCII, text);
    }

    public static final DefaultErrorMessage createUtf8(ByteBufAllocator alloc, CharSequence code,
            CharSequence message) {
        String text = code + " " + message;
        return new DefaultErrorMessage(ByteBufUtil.writeUtf8(alloc, text), AsciiString.of(code), message.toString(),
                CharsetUtil.UTF_8, text);
    }

    public static final DefaultErrorMessage create(ByteBufAllocator alloc, CharSequence code, CharSequence message,
            Charset charset) {
        String text = code + " " + message;
        return new DefaultErrorMessage(ByteBufUtil.encodeString(alloc, CharBuffer.wrap(text), charset),
                AsciiString.of(code), message.toString(), charset, text);
    }

    private final AsciiString code;
    private final String message;
    private final Charset charset;
    private final String text;

    private DefaultErrorMessage(ByteBuf content, AsciiString code, String message, Charset charset, String text) {
        super(content);
        this.code = code;
        this.message = message;
        this.charset = charset;
        this.text = text;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.ERROR;
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        out.add(type().content());
        out.add(content.retain());
        out.add(EOL_BUF.duplicate());
    }

    @Override
    public AsciiString code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public DefaultErrorMessage replace(ByteBuf content) {
        return new DefaultErrorMessage(content, code, message, charset, text);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + text() + "]";
    }

}
