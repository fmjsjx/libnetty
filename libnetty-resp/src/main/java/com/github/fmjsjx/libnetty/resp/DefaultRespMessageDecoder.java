package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.RespConstants.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

/**
 * The default implementation of {@link RespMessageDecoder}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class DefaultRespMessageDecoder extends RespMessageDecoder {

    static final class ArrayBuilder {

        private final int length;
        private final ArrayList<RespMessage> values;

        ArrayBuilder(int length) {
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

        RespArrayMessage<RespMessage> build() {
            return wrappedArray(values);
        }

    }

    private LinkedList<ArrayBuilder> nests;
    private int currentBulkStringLength;

    private final BiConsumer<ByteBuf, List<Object>> arrayHeaderDecoder = this::decodeArrayHeader;
    private final BiConsumer<ByteBuf, List<Object>> bulkStringLengthDecoder = this::decodeBulkStringLength;
    private final BiConsumer<ByteBuf, List<Object>> simpleStringDecoder = this::decodeSimpleString;
    private final BiConsumer<ByteBuf, List<Object>> errorDecoder = this::decodeError;
    private final BiConsumer<ByteBuf, List<Object>> integerDecoder = this::decodeInteger;
    
    /**
     * Constructs a new {@link DefaultRespMessageDecoder} using default
     * {@code maxInlineMessageLength} ({@code 65536}).
     */
    public DefaultRespMessageDecoder() {
        this(RespConstants.RESP_INLINE_MESSAGE_MAX_LENGTH);
    }

    /**
     * Constructs a new {@link DefaultRespMessageDecoder} using specified
     * {@code maxInlineMessageLength}.
     * 
     * @param maxInlineMessageLength maxInlineMessageLength the maximum length of
     *                               <b>in-line</b> messages
     */
    public DefaultRespMessageDecoder(int maxInlineMessageLength) {
        super(maxInlineMessageLength);
    }

    @Override
    protected void resetDecoder() {
        super.resetDecoder();
        nests = null;
    }

    @Override
    protected boolean decodeInline(ByteBuf in, List<Object> out) {
        byte typeValue = in.getByte(in.readerIndex());
        BiConsumer<ByteBuf, List<Object>> decoder;
        switch (typeValue) {
        case TYPE_ARRAY:
            decoder = arrayHeaderDecoder;
            break;
        case TYPE_BULK_STRING:
            decoder = bulkStringLengthDecoder;
            break;
        case TYPE_SIMPLE_STRING:
            decoder = simpleStringDecoder;
            break;
        case TYPE_ERROR:
            decoder = errorDecoder;
            break;
        case TYPE_INTEGER:
            decoder = integerDecoder;
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

    private void decodeSimpleString(ByteBuf inlineBytes, List<Object> out) {
        RespMessage msg = new DefaultSimpleStringMessage(inlineBytes.toString(CharsetUtil.UTF_8));
        appendMessage(msg, out);
    }

    private void appendMessage(RespMessage msg, List<Object> out) {
        if (nests == null) {
            out.add(msg);
            resetDecoder();
        } else {
            for (;;) {
                ArrayBuilder ab = nests.getLast();
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

    private void decodeBulkStringLength(ByteBuf inlineBytes, List<Object> out) {
        requireReadable(inlineBytes, NO_NUMBER_TO_PARSE);
        int length = parseLength(inlineBytes);
        if (length == -1) {
            appendMessage(RespMessages.nil(), out);
        } else {
            if (length > RESP_MESSAGE_MAX_LENGTH) {
                throw TOO_LONG_BULK_STRING_MESSAGE;
            }
            currentBulkStringLength = length;
            setState(State.DECODE_BULK_STRING_CONTENT);
        }
    }

    private void decodeArrayHeader(ByteBuf inlineBytes, List<Object> out) {
        int size = decodeArrayHeader(inlineBytes);
        if (size == 0) {
            appendMessage(RespMessages.emptyArray(), out);
        } else {
            if (nests == null) {
                nests = new LinkedList<>();
            }
            nests.addLast(new ArrayBuilder(size));
        }
    }

    @Override
    protected boolean decodeBulkStringContent(ByteBuf in, List<Object> out) {
        if (!in.isReadable(currentBulkStringLength + EOL_LENGTH)) {
            return false;
        }
        RespMessage msg;
        if (currentBulkStringLength == 0) {
            readEndOfLine(in);
            msg = RespMessages.emptyBulk();
        } else {
            ByteBuf content = in.readRetainedSlice(currentBulkStringLength);
            readEndOfLine(in);
            msg = new DefaultBulkStringMessage(content);
            setState(State.DECODE_INLINE);
        }
        appendMessage(msg, out);
        return true;
    }

}
