package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.POSITIVE_INT_MAX_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import java.util.List;
import java.util.function.Supplier;

import com.github.fmjsjx.libnetty.resp.RespCodecUtil.ToPositiveIntProcessor;
import com.github.fmjsjx.libnetty.resp.exception.RespDecoderException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

/**
 * Decodes {@link ByteBuf}s to {@link RespMessage}s.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class RespMessageDecoder extends ByteToMessageDecoder {

    protected static final RespDecoderException TOO_LONG_BULK_STRING_MESSAGE = new RespDecoderException(
            "too long Bulk String message");

    protected static final RespDecoderException NO_NUMBER_TO_PARSE = new RespDecoderException("no number to parse");

    protected static final RespDecoderException DECODING_OF_INLINE_COMMANDS_DISABLED = new RespDecoderException(
            "decoding of inline commands is disabled");

    protected static final void requireReadable(ByteBuf inlineBytes, Supplier<RespDecoderException> errorSupplier) {
        if (!inlineBytes.isReadable()) {
            throw errorSupplier.get();
        }
    }

    protected static final <E extends RespMessage> RespArrayMessage<E> wrappedArray(List<E> values) {
        return new DefaultArrayMessage<>(values);
    }

    protected static final ByteBuf readLine(ByteBuf in) {
        if (!in.isReadable(TYPE_LENGTH + EOL_LENGTH)) {
            return null;
        }
        final int lfIndex = in.forEachByte(ByteProcessor.FIND_LF);
        if (lfIndex < 0) {
            return null;
        }
        in.skipBytes(TYPE_LENGTH); // skip type
        ByteBuf data = in.readSlice(lfIndex - in.readerIndex() - 1); // `-1` is for CR
        readEndOfLine(in); // validate CR LF
        return data;
    }

    protected static final ByteBuf readInline(ByteBuf in) {
        if (!in.isReadable(EOL_LENGTH)) {
            return null;
        }
        final int lfIndex = in.forEachByte(ByteProcessor.FIND_LF);
        if (lfIndex < 0) {
            return null;
        }
        ByteBuf data = in.readSlice(lfIndex - in.readerIndex() - 1); // `-1` is for CR
        readEndOfLine(in); // validate CR LF
        return data;
    }

    protected static final void readEndOfLine(final ByteBuf in) {
        final short delim = in.readShort();
        if (RespConstants.EOL_SHORT == delim) {
            return;
        }
        final byte[] bytes = RespCodecUtil.shortToBytes(delim);
        throw new RespDecoderException("delimiter: [" + bytes[0] + "," + bytes[1] + "] (expected: \\r\\n)");
    }

    protected enum State {
        DECODE_INLINE, // [TYPE] SIMPLE_STRING | ERROR | INTEGER | ARRAY_HEADER | BULK_STRING_LENGTH
        DECODE_BULK_STRING_CONTENT // BULK_STRING_CONTENT
    }

    protected final int maxInlineMessageLength;

    protected State state = State.DECODE_INLINE;

    protected RespMessageDecoder(int maxInlineMessageLength) {
        this.maxInlineMessageLength = maxInlineMessageLength;
    }

    protected void checkInlineLength(ByteBuf inlineBytes) {
        if (inlineBytes.readableBytes() > maxInlineMessageLength) {
            throw new RespDecoderException(
                    "length: " + inlineBytes.readableBytes() + " (expected: <= " + maxInlineMessageLength + ")");
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            for (;;) {
                if (state == State.DECODE_INLINE) {
                    if (!in.isReadable()) {
                        return;
                    }
                    if (!decodeInline(in, out)) {
                        return;
                    }
                } else {
                    if (!decodeBulkStringContent(in, out)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            resetDecoder();
            throw e instanceof RespDecoderException ? e : new RespDecoderException(e);
        }
    }

    protected void resetDecoder() {
        state = State.DECODE_INLINE;
    }

    protected void setState(State state) {
        this.state = state;
    }

    protected abstract boolean decodeInline(ByteBuf in, List<Object> out);

    protected int decodeArrayHeader(ByteBuf inlineBytes) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        return parsePostiveInt(inlineBytes);
    }

    protected int parsePostiveInt(ByteBuf byteBuf) {
        ToPositiveIntProcessor numberProcessor = RespCodecUtil.toPositiveIntProcessor();
        byteBuf.forEachByte(numberProcessor);
        return numberProcessor.value();
    }

    protected int parseLength(ByteBuf byteBuf) {
        final int readableBytes = byteBuf.readableBytes();
        final boolean negative = readableBytes > 0 && byteBuf.getByte(byteBuf.readerIndex()) == '-';
        final int extraOneByteForNegative = negative ? 1 : 0;
        if (readableBytes <= extraOneByteForNegative) {
            throw new RespDecoderException("no number to parse: " + byteBuf.toString(CharsetUtil.US_ASCII));
        }
        if (readableBytes > POSITIVE_INT_MAX_LENGTH + extraOneByteForNegative) {
            throw new RespDecoderException(
                    "too many characters to be a valid RESP Integer: " + byteBuf.toString(CharsetUtil.US_ASCII));
        }
        if (negative) {
            return -parsePostiveInt(byteBuf.skipBytes(extraOneByteForNegative));
        }
        return parsePostiveInt(byteBuf);
    }

    protected abstract boolean decodeBulkStringContent(ByteBuf in, List<Object> out);

}
