package com.github.fmjsjx.libnetty.resp3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import com.github.fmjsjx.libnetty.resp.CachedErrorMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;
import com.github.fmjsjx.libnetty.resp.RespErrorMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp3.Resp3VerbatimStringMessage.Format;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;

/**
 * Provides static factory method for {@link Resp3Message}s.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class Resp3Messages {

    /**
     * Returns {@code nil}.
     * 
     * @return an {@code Resp3NullMessage}
     */
    public static final Resp3NullMessage nil() {
        return CachedNullMessage.INSTANCE;
    }

    /**
     * Returns an {@link Resp3BooleanMessage} instance with the specified value.
     * 
     * @param value the boolean value
     * @return a {@code Resp3BooleanMessage}
     */
    public static final Resp3BooleanMessage valueOf(boolean value) {
        return CachedBooleanMessage.valueOf(value);
    }

    /**
     * Returns an {@link Resp3DoubleMessage} instance with the specified value.
     * 
     * @param value the double value
     * @return an {@code Resp3DoubleMessage}
     */
    public static final Resp3DoubleMessage valueOf(double value) {
        if (Double.isNaN(value)) {
            throw RespCodecUtil.NaN;
        } else if (Double.POSITIVE_INFINITY == value) {
            return CachedPositiveInfinityMessage.INSTANCE;
        } else if (Double.NEGATIVE_INFINITY == value) {
            return CachedNegativeInfinityMesasge.INSTANCE;
        } else {
            return new DefaultDoubleMessage(new BigDecimal(value));
        }
    }

    /**
     * Returns positive infinity.
     * 
     * @return an {@code Resp3DoubleMessage}
     */
    public static final Resp3DoubleMessage positiveInfinity() {
        return CachedPositiveInfinityMessage.INSTANCE;
    }

    /**
     * Returns negative infinity.
     * 
     * @return an {@code Resp3DoubleMessage}
     */
    public static final Resp3DoubleMessage negativeInfinity() {
        return CachedNegativeInfinityMesasge.INSTANCE;
    }

    /**
     * Returns an {@link Resp3DoubleMessage} instance with the specified value.
     * 
     * @param value the double value
     * @return an {@code Resp3DoubleMessage}
     */
    public static final Resp3DoubleMessage valueOf(BigDecimal value) {
        return new DefaultDoubleMessage(value);
    }

    /**
     * Returns an {@link Resp3BigNumberMessage} instance with the specified value.
     * 
     * @param value the big integer value
     * @return an {@code Resp3BigNumberMessage}
     */
    public static final Resp3BigNumberMessage valueOf(BigInteger value) {
        return new DefaultBigNumberMessage(value);
    }

    /**
     * Returns an {@link Resp3BlobErrorMessage} instance with the {@code ERR} code
     * and the specified message.
     * 
     * @param message the error message
     * @return an {@code Resp3BlobErrorMessage}
     */
    public static final Resp3BlobErrorMessage blobError(String message) {
        return DefaultBlobErrorMessage.createErr(message);
    }

    /**
     * Returns an {@link Resp3BlobErrorMessage} instance with the specified code and
     * message.
     * 
     * @param code    the error code
     * @param message the error message
     * @return an {@code Resp3BlobErrorMessage}
     */
    public static final Resp3BlobErrorMessage blobError(CharSequence code, String message) {
        return DefaultBlobErrorMessage.create(code, message);
    }

    /**
     * Returns an {@link Resp3VerbatimStringMessage} instance with the text format
     * and the specified content.
     * 
     * @param content the content
     * @return an {@code Resp3VerbatimStringMessage}
     */
    public static final Resp3VerbatimStringMessage txt(ByteBuf content) {
        return new DefaultVerbatimStringMessage(Format.PLAIN_TEXT, content);
    }

    /**
     * Returns an {@link Resp3VerbatimStringMessage} instance with the text format
     * and the specified content.
     * 
     * @param alloc   the allocator allocates {@link ByteBuf}s
     * @param content the content
     * @return an {@code Resp3VerbatimStringMessage}
     */
    public static final Resp3VerbatimStringMessage txt(ByteBufAllocator alloc, String content) {
        byte[] b = Objects.requireNonNull(content, "content must not be null").getBytes(CharsetUtil.UTF_8);
        ByteBuf data = RespCodecUtil.buffer(b.length).writeBytes(b);
        return new DefaultVerbatimStringMessage(Format.PLAIN_TEXT, data);
    }

    /**
     * Returns an {@link Resp3VerbatimStringMessage} instance with the markdown
     * format and the specified content.
     * 
     * @param content the content
     * @return an {@code Resp3VerbatimStringMessage}
     */
    public static final Resp3VerbatimStringMessage markdown(ByteBuf content) {
        return new DefaultVerbatimStringMessage(Format.MARKDOWN, content);
    }

    /**
     * Returns an {@link Resp3VerbatimStringMessage} instance with the markdown
     * format and the specified content.
     * 
     * @param alloc   the allocator allocates {@link ByteBuf}s
     * @param content the content
     * @return an {@code Resp3VerbatimStringMessage}
     */
    public static final Resp3VerbatimStringMessage markdown(ByteBufAllocator alloc, String content) {
        byte[] b = Objects.requireNonNull(content, "content must not be null").getBytes(CharsetUtil.UTF_8);
        ByteBuf data = RespCodecUtil.buffer(b.length).writeBytes(b);
        return new DefaultVerbatimStringMessage(Format.MARKDOWN, data);
    }

    /**
     * Returns an unmodifiable empty {@link Resp3MapMessage}.
     * 
     * @param <F> the type of the field
     * @param <V> the type of the value
     * 
     * @return an unmodifiable empty {@link Resp3MapMessage}
     */
    public static final <F extends RespMessage, V extends RespMessage> Resp3MapMessage<F, V> emptyMap() {
        return Resp3MapMessage.of();
    }

    /**
     * Returns an unmodifiable {@link Resp3MapMessage} containing a single
     * {@link FieldValuePair}.
     * 
     * @param <F>   the type of the field
     * @param <V>   the type of the value
     * @param field the field
     * @param value the value
     * @return an unmodifiable {@link Resp3MapMessage} containing a single
     *         {@link FieldValuePair}
     * @since 1.2
     */
    public static final <F extends RespMessage, V extends RespMessage> Resp3MapMessage<F, V> map(F field, V value) {
        return Resp3MapMessage.of(field, value);
    }

    /**
     * Returns an unmodifiable empty {@link Resp3SetMessage}.
     * 
     * @param <E> the type of values in the message
     * @return an unmodifiable empty {@link Resp3SetMessage}
     */
    public static final <E extends RespMessage> Resp3SetMessage<E> emptySet() {
        return Resp3SetMessage.of();
    }

    /**
     * Returns an unmodifiable {@link Resp3SetMessage} containing a single value.
     * 
     * @param <E>   the type of values in the message
     * @param value the value
     * @return an unmodifiable {@link Resp3SetMessage} containing a single value
     */
    public static <E extends RespMessage> Resp3SetMessage<E> set(E value) {
        return Resp3SetMessage.of(value);
    }

    /**
     * Returns an {@link Resp3StreamedStringHeaderMessage} instance.
     * 
     * @return an {@code Resp3StreamedStringHeaderMessage}
     */
    public static final Resp3StreamedStringHeaderMessage streamedStringHeader() {
        return CachedStreamedStringHeaderMessage.INSTANCE;
    }

    /**
     * Returns an {@link Resp3StreamedStringPartMessage} instance as last streamed
     * string part.
     * 
     * @return an {@code Resp3StreamedStringPartMessage}
     */
    public static final Resp3StreamedStringPartMessage lastStreamedStringPart() {
        return CachedLastStreamedStringPartMessage.INSTANCE;
    }

    /**
     * Returns the header message of the unbound Array.
     * 
     * @return an {@code RespMessage}
     */
    public static final RespMessage unboundArrayHeader() {
        return CachedUnboundAggregateHeaderMessage.UNBOUND_ARRAY;
    }

    /**
     * Returns the header message of the unbound Map.
     * 
     * @return an {@code RespMessage}
     */
    public static final RespMessage unboundMapHeader() {
        return CachedUnboundAggregateHeaderMessage.UNBOUND_MAP;
    }

    /**
     * Returns the header message of the unbound Set.
     * 
     * @return an {@code RespMessage}
     */
    public static final RespMessage unboundSetHeader() {
        return CachedUnboundAggregateHeaderMessage.UNBOUND_SET;
    }

    /**
     * Returns the header message of the unbound Push.
     * 
     * @return an {@code RespMessage}
     */
    public static final RespMessage unboundPushHeader() {
        return CachedUnboundAggregateHeaderMessage.UNBOUND_PUSH;
    }

    /**
     * Returns an {@link Resp3EndMessage} instance.
     * 
     * @return an {@code Resp3EndMessage}
     */
    public static final Resp3EndMessage end() {
        return CachedEndMessage.INSTANCE;
    }

    private static final class WrongPassInstanceHolder {
        private static final CachedErrorMessage INSTANCE = CachedErrorMessage
                .createAscii("WRONGPASS invalid username-password pair");
    }

    /**
     * Returns the error:
     * 
     * <pre>
     * {@code
     * WRONGPASS invalid username-password pair
     * }
     * </pre>
     * 
     * @return a {@code RespErrorMessage}
     * 
     * @since 2.2.2
     */
    public static final RespErrorMessage wrongPass() {
        return WrongPassInstanceHolder.INSTANCE;
    }

}
