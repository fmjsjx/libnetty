package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.RESP_MESSAGE_MAX_LENGTH;

import java.util.ArrayList;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.exception.RespDecoderException;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

/**
 * Decodes {@link ByteBuf}s to {@link RedisRequest}s.
 *
 * @since 1.0
 * 
 * @author fmjsjx
 */
public class RedisRequestDecoder extends RespMessageDecoder {

    private static final RespDecoderException DECODING_OF_INLINE_COMMANDS_DISABLED = new RespDecoderException(
            "decoding of inline commands is disabled");

    private static final RespDecoderException REDIS_REQUEST_ELEMENTS_ONLY_SUPPORT_BULK_STRINGS = new RespDecoderException(
            "redis request elements only support Bulk Strings");

    private final boolean supportInlineCommand;

    private int arraySize;
    private int currentBulkStringLength;

    private ArrayList<RespBulkStringMessage> bulkStrings;

    /**
     * Constructs a new {@link RedisRequestDecoder} using default
     * {@code maxInlineMessageLength} ({@code 65536}) and does not support <b>inline
     * command</b>.
     */
    public RedisRequestDecoder() {
        this(false);
    }

    /**
     * Constructs a new {@link RedisRequestDecoder} using default
     * {@code maxInlineMessageLength} ({@code 65536}).
     * 
     * @param supportInlineCommand if {@code true} then this decoder will support
     *                             <b>inline command</b>
     */
    public RedisRequestDecoder(boolean supportInlineCommand) {
        this(supportInlineCommand, RespConstants.RESP_INLINE_MESSAGE_MAX_LENGTH);
    }

    /**
     * Constructs a new {@link RedisRequestDecoder} using specified
     * {@code maxInlineMessageLength}.
     * 
     * @param supportInlineCommand   if {@code true} then this decoder will support
     *                               <b>inline command</b>
     * @param maxInlineMessageLength the maximum length of the <b>inline message</b>
     */
    public RedisRequestDecoder(boolean supportInlineCommand, int maxInlineMessageLength) {
        super(maxInlineMessageLength);
        this.supportInlineCommand = supportInlineCommand;
    }

    @Override
    protected boolean decodeInline(ByteBuf in, List<Object> out) {
        byte typeValue = in.getByte(in.readerIndex());
        if (bulkStrings == null) {
            if (typeValue == RespMessageType.ARRAY.value()) {
                ByteBuf inlineBytes = readLine(in);
                if (inlineBytes == null) {
                    checkInlineLength(in);
                    return false;
                }
                decodeArrayHeader(inlineBytes, out);
            } else {
                if (supportInlineCommand) {
                    ByteBuf inlineBytes = readInline(in);
                    if (inlineBytes == null) {
                        checkInlineLength(in);
                        return false;
                    }
                    decodeInlineCommands(inlineBytes, out);
                } else {
                    throw DECODING_OF_INLINE_COMMANDS_DISABLED;
                }
            }
        } else {
            if (typeValue == RespMessageType.BULK_STRING.value()) {
                ByteBuf inlineBytes = readLine(in);
                if (inlineBytes == null) {
                    checkInlineLength(in);
                    return false;
                }
                decodeBulkStringLength(inlineBytes, out);
            } else {
                throw REDIS_REQUEST_ELEMENTS_ONLY_SUPPORT_BULK_STRINGS;
            }
        }
        return true;
    }

    @Override
    protected void resetDecoder() {
        super.resetDecoder();
        bulkStrings = null;
    }

    private void decodeArrayHeader(ByteBuf inlineBytes, List<Object> out) {
        int size = decodeArrayHeader(inlineBytes);
        if (size == 0) {
            out.add(RespMessages.emptyArray());
            resetDecoder();
        } else {
            arraySize = size;
            bulkStrings = new ArrayList<>(size);
            state = State.DECODE_INLINE;
        }
    }

    private void decodeBulkStringLength(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int length = parseLength(inlineBytes);
        if (length == -1) {
            bulkStrings.add(RespMessages.nil());
            if (bulkStrings.size() == arraySize) {
                outputRequest(out);
            } else {
                state = State.DECODE_INLINE;
            }
        } else {
            currentBulkStringLength = length;
            if (currentBulkStringLength > RESP_MESSAGE_MAX_LENGTH) {
                throw TOO_LONG_BULK_STRING_MESSAGE;
            }
            state = State.DECODE_BULK_STRING_CONTENT;
        }
    }

    private void decodeInlineCommands(ByteBuf inlineBytes, List<Object> out) {
        List<RespBulkStringMessage> commands = new ArrayList<>();
        for (; inlineBytes.isReadable();) {
            int begin = inlineBytes.forEachByte(ByteProcessor.FIND_NON_LINEAR_WHITESPACE);
            if (begin == -1) {
                break;
            }
            inlineBytes.readerIndex(begin);
            int end = inlineBytes.forEachByte(ByteProcessor.FIND_LINEAR_WHITESPACE);
            if (end == -1) {
                commands.add(new DefaultBulkStringMessage(inlineBytes.readRetainedSlice(inlineBytes.readableBytes())));
                break;
            } else {
                commands.add(new DefaultBulkStringMessage(inlineBytes.readRetainedSlice(end - begin)));
            }
        }
        if (commands.isEmpty()) {
            out.add(new RedisRequest(RespMessages.emptyArray()));
        } else {
            out.add(new RedisRequest(new DefaultArrayMessage(commands)));
        }
        resetDecoder();
    }

    private void outputRequest(List<Object> out) {
        out.add(new RedisRequest(new DefaultArrayMessage(bulkStrings)));
        resetDecoder();
    }

    protected boolean decodeBulkStringContent(ByteBuf in, List<Object> out) {
        if (!in.isReadable(currentBulkStringLength + EOL_LENGTH)) {
            return false;
        }
        if (currentBulkStringLength == 0) {
            readEndOfLine(in);
            bulkStrings.add(RespMessages.emptyBulk());
        } else {
            ByteBuf content = in.readRetainedSlice(currentBulkStringLength);
            readEndOfLine(in);
            bulkStrings.add(new DefaultBulkStringMessage(content));
        }
        if (bulkStrings.size() == arraySize) {
            outputRequest(out);
        } else {
            state = State.DECODE_INLINE;
        }
        return true;
    }

}
