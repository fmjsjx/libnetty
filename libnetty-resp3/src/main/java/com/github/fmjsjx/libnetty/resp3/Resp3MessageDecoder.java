package com.github.fmjsjx.libnetty.resp3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import com.github.fmjsjx.libnetty.resp.DefaultBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.DefaultErrorMessage;
import com.github.fmjsjx.libnetty.resp.DefaultIntegerMessage;
import com.github.fmjsjx.libnetty.resp.DefaultSimpleStringMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;
import com.github.fmjsjx.libnetty.resp.RespConstants;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageDecoder;
import com.github.fmjsjx.libnetty.resp.RespMessages;
import com.github.fmjsjx.libnetty.resp.exception.RespDecoderException;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

/**
 * Decodes {@link ByteBuf}s to {@link RespMessage}s (include
 * {@link Resp3Message}s).
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class Resp3MessageDecoder extends RespMessageDecoder {

    private static final RespDecoderException UNBOUND_MUST_AT_TOP_LEVEL = new RespDecoderException(
            "unbound aggregate types must at top level");

    private static final RespDecoderException NO_VALUE_TO_PARSE = new RespDecoderException("no value to parse");

    protected static final RespDecoderException TOO_LONG_CONTENT_MESSAGE = new RespDecoderException(
            "too long content message");

    private static final ByteProcessor FIND_NON_NUMBER = v -> v >= '0' && v <= '9';

    static final class AggregateBuilder {

        private final byte type;
        private final int length;
        private final ArrayList<RespMessage> values;

        private AggregateBuilder(byte type, int length) {
            this.type = type;
            if (type == Resp3Constants.TYPE_MAP || type == Resp3Constants.TYPE_ATTRIBUTE) {
                length = length << 1;
            }
            this.length = length;
            this.values = new ArrayList<>(length);
        }

        boolean isReachedEnd() {
            return values.size() == length;
        }

        boolean add(RespMessage msg) {
            values.add(msg);
            return isReachedEnd();
        }

        RespMessage build() {
            ArrayList<RespMessage> values = this.values;
            switch (type) {
            default:
            case RespConstants.TYPE_ARRAY:
                return wrappedArray(values);
            case Resp3Constants.TYPE_MAP:
                if (length == 2) {
                    return Resp3MapMessage.of(values.get(0), values.get(1));
                }
                return new DefaultMapMessage<>(toPairs());
            case Resp3Constants.TYPE_SET:
                if (length == 1) {
                    return Resp3SetMessage.of(values.get(0));
                }
                return new DefaultSetMessage<>(values);
            case Resp3Constants.TYPE_ATTRIBUTE:
                if (length == 2) {
                    return Resp3AttributeMessage.of(values.get(0), values.get(1));
                }
                return new DefaultAttributeMessage<>(toPairs());
            case Resp3Constants.TYPE_PUSH:
                return new DefaultPushMessage<>(values);
            }
        }

        List<FieldValuePair<RespMessage, RespMessage>> toPairs() {
            int len = length >>> 1;
            ArrayList<RespMessage> values = this.values;
            List<FieldValuePair<RespMessage, RespMessage>> pairs = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                RespMessage field = values.get(i);
                RespMessage value = values.get(i + 1);
                pairs.add(new FieldValuePair<>(field, value));
            }
            return pairs;
        }

    }

    private LinkedList<AggregateBuilder> nests;
    private byte currentContentType;
    private int currentContentLength;

    private final BiConsumer<ByteBuf, List<Object>> arrayHeaderDecoder = this::decodeArrayHeader;
    private final BiConsumer<ByteBuf, List<Object>> bulkStringLengthDecoder = this::decodeBulkStringLength;
    private final BiConsumer<ByteBuf, List<Object>> simpleStringDecoder = this::decodeSimpleString;
    private final BiConsumer<ByteBuf, List<Object>> errorDecoder = this::decodeError;
    private final BiConsumer<ByteBuf, List<Object>> integerDecoder = this::decodeInteger;
    private final BiConsumer<ByteBuf, List<Object>> nullDecoder = this::decodeNull;
    private final BiConsumer<ByteBuf, List<Object>> doubleDecoder = this::decodeDouble;
    private final BiConsumer<ByteBuf, List<Object>> booleanDecoder = this::decodeBoolean;
    private final BiConsumer<ByteBuf, List<Object>> blobErrorLengthDecoder = this::decodeBlobErrorLength;
    private final BiConsumer<ByteBuf, List<Object>> verbatimStringLengthDecoder = this::decodeVerbatimStringLength;
    private final BiConsumer<ByteBuf, List<Object>> bigNumberDecoder = this::decodeBigNumber;
    private final BiConsumer<ByteBuf, List<Object>> mapHeaderDecoder = this::decodeMapHeader;
    private final BiConsumer<ByteBuf, List<Object>> setHeaderDecoder = this::decodeSetHeader;
    private final BiConsumer<ByteBuf, List<Object>> attributeHeaderDecoder = this::decodeAttributeHeader;
    private final BiConsumer<ByteBuf, List<Object>> pushHeaderDecoder = this::decodePushHeader;
    private final BiConsumer<ByteBuf, List<Object>> streamedStringPartLengthDecoder = this::decodeStreamedStringPartLength;
    private final BiConsumer<ByteBuf, List<Object>> endDecoder = this::decodeEnd;

    /**
     * Constructs a new {@link Resp3MessageDecoder} using default
     * {@code maxInlineMessageLength} ({@code 65536}).
     */
    public Resp3MessageDecoder() {
        this(RespConstants.RESP_INLINE_MESSAGE_MAX_LENGTH);
    }

    /**
     * Constructs a new {@link Resp3MessageDecoder} using specified
     * {@code maxInlineMessageLength}.
     * 
     * @param maxInlineMessageLength maxInlineMessageLength the maximum length of
     *                               <b>in-line</b> messages
     */
    public Resp3MessageDecoder(int maxInlineMessageLength) {
        super(maxInlineMessageLength);
    }

    @Override
    protected void resetDecoder() {
        super.resetDecoder();
        nests = null;
    }

    @Override
    protected boolean decodeInline(ByteBuf in, List<Object> out) {
        BiConsumer<ByteBuf, List<Object>> decoder;
        byte typeValue = in.getByte(in.readerIndex());
        switch (typeValue) {
        case RespConstants.TYPE_ARRAY:
            decoder = arrayHeaderDecoder;
            break;
        case RespConstants.TYPE_BULK_STRING:
            decoder = bulkStringLengthDecoder;
            break;
        case RespConstants.TYPE_SIMPLE_STRING:
            decoder = simpleStringDecoder;
            break;
        case RespConstants.TYPE_ERROR:
            decoder = errorDecoder;
            break;
        case RespConstants.TYPE_INTEGER:
            decoder = integerDecoder;
            break;
        case Resp3Constants.TYPE_NULL:
            decoder = nullDecoder;
            break;
        case Resp3Constants.TYPE_DOUBLE:
            decoder = doubleDecoder;
            break;
        case Resp3Constants.TYPE_BOOLEAN:
            decoder = booleanDecoder;
            break;
        case Resp3Constants.TYPE_BLOB_ERROR:
            decoder = blobErrorLengthDecoder;
            break;
        case Resp3Constants.TYPE_VERBATIM_STRING:
            decoder = verbatimStringLengthDecoder;
            break;
        case Resp3Constants.TYPE_BIG_NUMBER:
            decoder = bigNumberDecoder;
            break;
        case Resp3Constants.TYPE_MAP:
            decoder = mapHeaderDecoder;
            break;
        case Resp3Constants.TYPE_SET:
            decoder = setHeaderDecoder;
            break;
        case Resp3Constants.TYPE_ATTRIBUTE:
            decoder = attributeHeaderDecoder;
            break;
        case Resp3Constants.TYPE_PUSH:
            decoder = pushHeaderDecoder;
            break;
        case Resp3Constants.TYPE_STREAMED_STRING_PART:
            decoder = streamedStringPartLengthDecoder;
            break;
        case Resp3Constants.TYPE_END:
            decoder = endDecoder;
            break;
        default: // INLINE COMMAND
            throw DECODING_OF_INLINE_COMMANDS_DISABLED;
        }
        ByteBuf inlineBytes = readLine(in);
        if (inlineBytes == null) {
            checkInlineLength(in);
            return false;
        }
        decoder.accept(inlineBytes, out);
        return true;
    }

    private void decodeArrayHeader(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        if (isUnbound(inlineBytes)) {
            if (nests != null) {
                throw UNBOUND_MUST_AT_TOP_LEVEL;
            }
            out.add(Resp3Messages.unboundArrayHeader());
            return;
        }
        int size = parsePostiveInt(inlineBytes);
        if (size == 0) {
            appendMessage(RespMessages.emptyArray(), out);
        } else {
            ensureNests().addLast(new AggregateBuilder(RespConstants.TYPE_ARRAY, size));
        }
    }

    private LinkedList<AggregateBuilder> ensureNests() {
        LinkedList<AggregateBuilder> nests = this.nests;
        if (nests == null) {
            this.nests = nests = new LinkedList<>();
        }
        return nests;
    }

    boolean isUnbound(ByteBuf inlineBytes) {
        return inlineBytes.getByte(inlineBytes.readerIndex()) == '?';
    }

    private void appendMessage(RespMessage msg, List<Object> out) {
        LinkedList<AggregateBuilder> nests = this.nests;
        if (nests == null) {
            out.add(msg);
            resetDecoder();
        } else {
            for (;;) {
                AggregateBuilder ab = nests.getLast();
                if (ab.add(msg)) {
                    msg = ab.build();
                    nests.removeLast();
                    if (nests.isEmpty()) {
                        out.add(msg);
                        resetDecoder();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
    }

    private void decodeBulkStringLength(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        if (isUnbound(inlineBytes)) {
            if (nests != null) {
                throw UNBOUND_MUST_AT_TOP_LEVEL;
            }
            out.add(Resp3Messages.streamedStringHeader());
            return;
        }
        int length = parseLength(inlineBytes);
        if (length == -1) {
            appendMessage(RespMessages.nil(), out);
        } else {
            if (length > RespConstants.RESP_MESSAGE_MAX_LENGTH) {
                throw TOO_LONG_BULK_STRING_MESSAGE;
            }
            currentContentType = RespConstants.TYPE_BULK_STRING;
            currentContentLength = length;
            setState(State.DECODE_BULK_STRING_CONTENT);
        }
    }

    private void decodeSimpleString(ByteBuf inlineBytes, List<Object> out) {
        RespMessage msg = new DefaultSimpleStringMessage(inlineBytes.toString(CharsetUtil.UTF_8));
        appendMessage(msg, out);
    }

    private void decodeError(ByteBuf inlineBytes, List<Object> out) {
        String text = inlineBytes.toString(CharsetUtil.UTF_8);
        int codeLength = inlineBytes.bytesBefore(RespConstants.SPACE);
        if (codeLength == -1) {
            // no space char found
            appendMessage(new DefaultErrorMessage(text, "", text), out);
        } else {
            String code = text.substring(0, codeLength);
            String message = text.substring(codeLength + 1);
            appendMessage(new DefaultErrorMessage(code, message, text), out);
        }
    }

    private void decodeInteger(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        long value = RespCodecUtil.decodeLong(inlineBytes);
        RespMessage msg = new DefaultIntegerMessage(value);
        appendMessage(msg, out);
    }

    private void decodeNull(ByteBuf inlineBytes, List<Object> out) {
        appendMessage(Resp3Messages.nil(), out);
    }

    private void decodeDouble(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        String value = inlineBytes.toString(CharsetUtil.US_ASCII);
        switch (value) {
        case "inf":
            appendMessage(CachedPositiveInfinityMessage.INSTANCE, out);
            break;
        case "-inf":
            appendMessage(CachedNegativeInfinityMesasge.INSTANCE, out);
            break;
        default:
            appendMessage(new DefaultDoubleMessage(value), out);
            break;
        }
    }

    private void decodeBoolean(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_VALUE_TO_PARSE);
        byte value = inlineBytes.getByte(inlineBytes.readerIndex());
        switch (value) {
        case Resp3Constants.TRUE_VALUE:
            appendMessage(CachedBooleanMessage.TRUE, out);
            break;
        case Resp3Constants.FALSE_VALUE:
            appendMessage(CachedBooleanMessage.FALSE, out);
            break;
        default:
            throw new RespDecoderException("Unknown boolean value '" + ((char) value) + "'");
        }
    }

    private void decodeBlobErrorLength(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int length = parsePostiveInt(inlineBytes);
        if (length > RespConstants.RESP_MESSAGE_MAX_LENGTH) {
            throw TOO_LONG_BULK_STRING_MESSAGE;
        }
        currentContentType = Resp3Constants.TYPE_BLOB_ERROR;
        currentContentLength = length;
        setState(State.DECODE_BULK_STRING_CONTENT);
    }

    private void decodeVerbatimStringLength(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int length = parsePostiveInt(inlineBytes);
        if (length > RespConstants.RESP_MESSAGE_MAX_LENGTH) {
            throw TOO_LONG_BULK_STRING_MESSAGE;
        }
        currentContentType = Resp3Constants.TYPE_VERBATIM_STRING;
        currentContentLength = length;
        setState(State.DECODE_BULK_STRING_CONTENT);
    }

    private void decodeBigNumber(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        if (inlineBytes.forEachByte(FIND_NON_NUMBER) != -1) {
            throw RespCodecUtil.NaN;
        }
        RespMessage msg = DefaultBigNumberMessage.create(inlineBytes.toString(CharsetUtil.US_ASCII));
        appendMessage(msg, out);
    }

    private void decodeMapHeader(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        if (isUnbound(inlineBytes)) {
            if (nests != null) {
                throw UNBOUND_MUST_AT_TOP_LEVEL;
            }
            out.add(Resp3Messages.unboundMapHeader());
            return;
        }
        int size = parsePostiveInt(inlineBytes);
        if (size == 0) {
            appendMessage(Resp3Messages.emptyMap(), out);
        } else {
            LinkedList<AggregateBuilder> nests = ensureNests();
            nests.addLast(new AggregateBuilder(Resp3Constants.TYPE_MAP, size));
        }
    }

    private void decodeSetHeader(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        if (isUnbound(inlineBytes)) {
            if (nests != null) {
                throw UNBOUND_MUST_AT_TOP_LEVEL;
            }
            out.add(Resp3Messages.unboundSetHeader());
            return;
        }
        int size = parsePostiveInt(inlineBytes);
        if (size == 0) {
            appendMessage(Resp3Messages.emptySet(), out);
        } else {
            ensureNests().addLast(new AggregateBuilder(Resp3Constants.TYPE_SET, size));
        }
    }

    private void decodeAttributeHeader(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int size = parsePostiveInt(inlineBytes);
        if (size == 0) {
            appendMessage(new DefaultAttributeMessage<>(), out);
        } else {
            ensureNests().addLast(new AggregateBuilder(Resp3Constants.TYPE_ATTRIBUTE, size));
        }
    }

    private void decodePushHeader(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        if (isUnbound(inlineBytes)) {
            if (nests != null) {
                throw UNBOUND_MUST_AT_TOP_LEVEL;
            }
            out.add(Resp3Messages.unboundPushHeader());
            return;
        }
        int size = parsePostiveInt(inlineBytes);
        if (size == 0) {
            appendMessage(new DefaultPushMessage<>(), out);
        } else {
            ensureNests().addLast(new AggregateBuilder(Resp3Constants.TYPE_PUSH, size));
        }
    }

    private void decodeStreamedStringPartLength(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int length = parsePostiveInt(inlineBytes);
        if (length > RespConstants.RESP_MESSAGE_MAX_LENGTH) {
            throw TOO_LONG_BULK_STRING_MESSAGE;
        }
        currentContentType = Resp3Constants.TYPE_STREAMED_STRING_PART;
        currentContentLength = length;
        setState(State.DECODE_BULK_STRING_CONTENT);
    }

    private void decodeEnd(ByteBuf inlineBytes, List<Object> out) {
        appendMessage(Resp3Messages.end(), out);
    }

    @Override
    protected boolean decodeBulkStringContent(ByteBuf in, List<Object> out) {
        // For RESP3, all content decode logic should write at this method
        // BulkString, BlobError, VerbatimString
        int length = currentContentLength;
        if (!in.isReadable(length + RespConstants.EOL_LENGTH)) {
            return false;
        }
        RespMessage msg;
        switch (currentContentType) {
        default:
        case RespConstants.TYPE_BULK_STRING:
            if (length == 0) {
                msg = RespMessages.emptyBulk();
            } else {
                msg = new DefaultBulkStringMessage(in.readRetainedSlice(length));
            }
            readEndOfLine(in);
            break;
        case Resp3Constants.TYPE_BLOB_ERROR:
            msg = decodeBlobError(in.readSlice(length));
            readEndOfLine(in);
            break;
        case Resp3Constants.TYPE_VERBATIM_STRING:
            if (length < 4) {
                throw new RespDecoderException("length of verbatim string must >= 4");
            }
            String formatPart = in.toString(in.readerIndex(), 3, CharsetUtil.US_ASCII);
            in.skipBytes(4);
            msg = new DefaultVerbatimStringMessage(formatPart, in.readRetainedSlice(length - 4));
            readEndOfLine(in);
            break;
        }
        setState(State.DECODE_INLINE);
        appendMessage(msg, out);
        return true;
    }

    private Resp3BlobErrorMessage decodeBlobError(ByteBuf in) {
        String text = in.toString(CharsetUtil.UTF_8);
        int codeLength = in.bytesBefore(RespConstants.SPACE);
        if (codeLength == -1) {
            // no space char found
            return new DefaultBlobErrorMessage(text, "", text);
        } else {
            String code = text.substring(0, codeLength);
            String message = text.substring(codeLength + 1);
            return new DefaultBlobErrorMessage(AsciiString.cached(code), message, text);
        }
    }

}
