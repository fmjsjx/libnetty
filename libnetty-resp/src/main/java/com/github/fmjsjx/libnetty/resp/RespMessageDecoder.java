package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.github.fmjsjx.libnetty.resp.RespCodecUtil.ToPositiveIntProcessor;
import com.github.fmjsjx.libnetty.resp.exception.RespDecoderException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;

public abstract class RespMessageDecoder extends ByteToMessageDecoder {

    protected static final RespDecoderException TOO_LONG_BULK_STRING_MESSAGE = new RespDecoderException(
            "too long Bulk String message");

    protected static final RespDecoderException DECODING_OF_INLINE_COMMANDS_DISABLED = new RespDecoderException(
            "decoding of inline commands is disabled");

    protected static final RespDecoderException NO_NUMBER_TO_PARSE = new RespDecoderException("no number to parse");

    protected static final void requireReadable(ByteBuf inlineBytes, Supplier<RespDecoderException> errorSupplier) {
        if (!inlineBytes.isReadable()) {
            throw errorSupplier.get();
        }
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

    protected final ToPositiveIntProcessor toPositiveIntProcessor = new ToPositiveIntProcessor();

    protected final int maxInlineMessageLength;

    protected State state = State.DECODE_INLINE;

    protected int arraySize;
    protected int currentBulkStringLength;

    protected ArrayList<RespBulkStringMessage> bulkStrings;

    public RespMessageDecoder(int maxInlineMessageLength, boolean useCompositeCumulator) {
        this.maxInlineMessageLength = maxInlineMessageLength;
        if (useCompositeCumulator) {
            setCumulator(COMPOSITE_CUMULATOR);
        }
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
        bulkStrings = null;
    }

    protected boolean decodeInline(ByteBuf in, List<Object> out) {
        byte typeValue = in.getByte(in.readerIndex());
        switch (typeValue) {
        case TYPE_ARRAY:
            ByteBuf inlineBytes = readLine(in);
            if (inlineBytes == null) {
                checkInlineLength(in);
                return false;
            }
            decodeArrayHeader(inlineBytes, out);
            break;
        case TYPE_BULK_STRING:
            // TODO bulk string
            break;
        case TYPE_INTEGER:
            // TODO integer
            break;
        case TYPE_SIMPLE_STRING:
            // TODO simple string
            break;
        case TYPE_ERROR:
            // TODO error
            break;
        default:
            // TODO in-line commands
            break;
        }
        return true;
    }

    protected void decodeArrayHeader(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int size = parsePostiveInt(inlineBytes);
        if (size == 0) {
            out.add(RespMessages.emptyArray());
            resetDecoder();
        } else {
            arraySize = size;
            bulkStrings = new ArrayList<>(size);
            state = State.DECODE_INLINE;
        }
    }

    protected int parsePostiveInt(ByteBuf byteBuf) {
        toPositiveIntProcessor.reset();
        byteBuf.forEachByte(toPositiveIntProcessor);
        return toPositiveIntProcessor.value();
    }

    protected abstract boolean decodeBulkStringContent(ByteBuf in, List<Object> out);

}
