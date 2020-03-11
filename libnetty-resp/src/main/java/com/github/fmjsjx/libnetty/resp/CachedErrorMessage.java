package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class CachedErrorMessage extends AbstractCachedRespMessage<CachedErrorMessage> implements RespErrorMessage {

    public static final CachedErrorMessage createAscii(CharSequence text) {
        String[] error = text.toString().split(" ", 2);
        if (ERR.toString().equalsIgnoreCase(error[0])) {
            return createErr(error[1], CharsetUtil.US_ASCII);
        }
        return createAscii(error[0], error[1]);
    }

    public static final CachedErrorMessage createAscii(CharSequence code, String message) {
        return create(code, message, CharsetUtil.US_ASCII);
    }

    public static final CachedErrorMessage createErrAscii(String message) {
        return createErr(message, CharsetUtil.US_ASCII);
    }

    public static final CachedErrorMessage createErr(String message, Charset charset) {
        return create(ERR, message, charset);
    }

    public static final CachedErrorMessage create(CharSequence code, String message, Charset charset) {
        if (code instanceof AsciiString) {
            return create((AsciiString) code, message, charset);
        }
        return create(new AsciiString(code), message, charset);
    }

    private static final CachedErrorMessage create(AsciiString code, String message, Charset charset) {
        String text = code.toUpperCase() + " " + message;
        byte[] bytes = text.getBytes(charset);
        int length = bytes.length;
        ByteBuf fullContent = fixedBuffer(length).writeBytes(RespMessageType.ERROR.content()).writeBytes(bytes)
                .writeShort(EOL_SHORT).asReadOnly();
        ByteBuf content = fullContent.slice(fullContent.readerIndex() + TYPE_LENGTH, length);
        return new CachedErrorMessage(content, fullContent, code, message, charset, text);
    }

    private final AsciiString code;
    private final String message;
    private final Charset charset;
    private final String text;

    private CachedErrorMessage(ByteBuf content, ByteBuf fullContent, AsciiString code, String message, Charset charset,
            String text) {
        super(content, fullContent);
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
    public CachedErrorMessage replace(ByteBuf content) {
        return new CachedErrorMessage(content, fullContent, code, message, charset, text);
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
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + text() + "]";
    }

}
