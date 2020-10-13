package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * The cached implementation of the {@link RespErrorMessage}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class CachedErrorMessage extends CachedRespMessage implements RespErrorMessage {

    /**
     * Returns a new {@link CachedErrorMessage} by the full {@code text} given.
     * 
     * @param text the full text of the error
     * @return a {@code CachedErrorMessage}
     */
    public static final CachedErrorMessage createAscii(CharSequence text) {
        String[] error = text.toString().split(" ", 2);
        if (ERR.toString().equalsIgnoreCase(error[0])) {
            return createErrAscii(error[1]);
        }
        return createAscii(error[0], error[1]);
    }

    /**
     * Returns a new {@link CachedErrorMessage} with the specific {@code code} and
     * {@code message} given.
     * 
     * @param code    the code of the error
     * @param message the message of the error
     * @return a {@code CachedErrorMessage}
     */
    public static final CachedErrorMessage createAscii(CharSequence code, String message) {
        return create(code, message, CharsetUtil.US_ASCII);
    }

    /**
     * Returns a new {@link CachedErrorMessage} with the {@code ERR} code and the
     * specific {@code message} given.
     * 
     * @param message the message of the error
     * @return a {@code CachedErrorMessage}
     */
    public static final CachedErrorMessage createErrAscii(String message) {
        return createAscii(ERR, message);
    }

    /**
     * Returns a new {@link CachedErrorMessage} with the specific {@code code} and
     * {@code message} given.
     * 
     * @param code    the code of the error
     * @param message the message of the error
     * @return a {@code CachedErrorMessage}
     */
    public static final CachedErrorMessage create(CharSequence code, String message) {
        return create(code, message, CharsetUtil.UTF_8);
    }

    private static final CachedErrorMessage create(CharSequence code, String message, Charset charset) {
        if (code instanceof AsciiString) {
            return create((AsciiString) code, message, charset);
        }
        return create(new AsciiString(code), message, charset);
    }

    private static final CachedErrorMessage create(AsciiString code, String message, Charset charset) {
        code = code.toUpperCase();
        String text = code + " " + message;
        byte[] bytes = text.getBytes(charset);
        int length = bytes.length;
        ByteBuf fullContent = RespCodecUtil.buffer(TYPE_LENGTH + length + EOL_LENGTH)
                .writeByte(RespMessageType.ERROR.value()).writeBytes(bytes).writeShort(EOL_SHORT).asReadOnly();
        return new CachedErrorMessage(fullContent, code, message, text);
    }

    private final CharSequence code;
    private final String message;
    private final String text;

    private CachedErrorMessage(ByteBuf fullContent, CharSequence code, String message, String text) {
        super(fullContent);
        this.code = code;
        this.message = message;
        this.text = text;
    }

    @Override
    public RespMessageType type() {
        return RespMessageType.ERROR;
    }

    @Override
    public CharSequence code() {
        return code;
    }

    @Override
    public String message() {
        return message;
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
