package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.POSITIVE_INT_MAX_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.RESP_MESSAGE_MAX_LENGTH;

import java.util.ArrayList;
import java.util.List;

import com.github.fmjsjx.libnetty.resp.exception.RespDecoderException;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

public class RedisRequestDecoder extends RespMessageDecoder {

    private static final RespDecoderException REDIS_REQUEST_ELEMENTS_ONLY_SUPPORT_BULK_STRINGS = new RespDecoderException(
            "redis request elements only support Bulk Strings");

    private ArrayList<RespBulkStringMessage> bulkStrings;

    private final boolean supportInlineCommand;

    public RedisRequestDecoder() {
        this(false);
    }

    public RedisRequestDecoder(boolean supportInlineCommand) {
        this(supportInlineCommand, RespConstants.RESP_INLINE_MESSAGE_MAX_LENGTH);
    }

    public RedisRequestDecoder(boolean supportInlineCommand, int maxInlineMessageLength) {
        this(supportInlineCommand, maxInlineMessageLength, false);
    }

    public RedisRequestDecoder(boolean supportInlineCommand, int maxInlineMessageLength,
            boolean useCompositeCumulator) {
        super(maxInlineMessageLength, useCompositeCumulator);
        this.supportInlineCommand = supportInlineCommand;
    }

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

    private int parseLength(ByteBuf byteBuf) {
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
