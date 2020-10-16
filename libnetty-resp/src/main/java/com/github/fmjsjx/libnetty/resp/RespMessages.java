package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.CachedRespMessages.*;

import java.util.Optional;

/**
 * Provides static factory method for {@link RespMessage}s.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class RespMessages {

    /**
     * Returns {@code +OK}.
     * 
     * @return a {@code RespSimpleStringMessage}
     */
    public static final RespSimpleStringMessage ok() {
        return OK;
    }

    /**
     * Returns {@code +PONG}.
     * 
     * @return a {@code RespSimpleStringMessage}
     */
    public static final RespSimpleStringMessage pong() {
        return PONG;
    }

    /**
     * Returns {@code nil}.
     * 
     * @return a {@code RespBulkStringMessage}
     */
    public static final RespBulkStringMessage nil() {
        return NULL;
    }

    /**
     * Returns {@code :0};
     * 
     * @return a {@code RespIntegerMessage}
     */
    public static final RespIntegerMessage zero() {
        return ZERO;
    }

    /**
     * Returns {@code :1};
     * 
     * @return a {@code RespIntegerMessage}
     */
    public static final RespIntegerMessage one() {
        return ONE;
    }

    /**
     * Returns the {@link RespIntegerMessage} with the specified {@code value}.
     * 
     * @param value the value
     * @return a {@code RespIntegerMessage}
     */
    public static final RespIntegerMessage integer(int value) {
        Optional<CachedIntegerMessage> optionalInteger = cachedIntegerMessage(value);
        if (optionalInteger.isPresent()) {
            return optionalInteger.get();
        } else {
            return new DefaultIntegerMessage(value);
        }
    }

    /**
     * Returns the {@link RespIntegerMessage} with the specified {@code value}.
     * 
     * @param value the value
     * @return a {@code RespIntegerMessage}
     */
    public static final RespIntegerMessage integer(long value) {
        Optional<CachedIntegerMessage> optionalInteger = cachedIntegerMessage(value);
        if (optionalInteger.isPresent()) {
            return optionalInteger.get();
        } else {
            return new DefaultIntegerMessage(value);
        }
    }

    /**
     * Returns the error:
     * 
     * <pre>
     * {@code
     *     -NOAUTH Authentication required.
     * }
     * </pre>
     * 
     * @return a {@code RespErrorMessage}
     */
    public static final RespErrorMessage noauth() {
        return NOAUTH;
    }

    /**
     * Returns the error:
     * 
     * <pre>
     * {@code
     *     -ERR value is not an integer or out of range
     * }
     * </pre>
     * 
     * @return a {@code RespErrorMessage}
     */
    public static final RespErrorMessage valueIsNotAnIntegerOrOutOfRange() {
        return ERR_VALUE_IS_NOT_AN_INTEGER_OR_OUT_OF_RANGE;
    }

    /**
     * Returns the error:
     * 
     * <pre>
     * {@code
     *     -ERR increment or decrement would overflow
     * }
     * </pre>
     * 
     * @return a {@code RespErrorMessage}
     */
    public static final RespErrorMessage incrementOrDecrementWouldOverflow() {
        return ERR_INCREMENT_OR_DECREMENT_WOULD_OVERFLOW;
    }

    /**
     * Returns the error:
     * 
     * <pre>
     * {@code
     *     -ERR wrong number of arguments for '$command' command
     * }
     * </pre>
     * 
     * @param command the command text
     * 
     * @return a {@code RespErrorMessage}
     */
    public static final RespErrorMessage wrongNumberOfArgumentsForCommand(String command) {
        return CachedRespMessages.cachedWrongNumberOfArgumentsForCommand(command);
    }

    /**
     * Returns the empty {@link RespArrayMessage}.
     * 
     * @param <E> the type of elements in the array
     * @return an empty {@code RespArrayMessage}
     */
    public static final <E extends RespMessage> RespArrayMessage<E> emptyArray() {
        return CachedEmptyArrayMessage.getInstance();
    }

    /**
     * Returns the empty {@link RespBulkStringMessage}.
     * 
     * @return an empty {@code RespBulkStringMessage}
     */
    public static final RespBulkStringMessage emptyBulk() {
        return EMPTY_BULK;
    }

    private RespMessages() {
    }

}
