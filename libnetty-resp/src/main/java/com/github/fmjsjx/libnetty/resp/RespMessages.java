package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.CachedRespMessages.*;

import java.util.Optional;

import io.netty.buffer.ByteBufAllocator;

/**
 * Provides static factory method for {@link RespMessage}s.
 * 
 * @since 1.0
 *
 * @author fmjsjx
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

    public static final RespIntegerMessage integer(ByteBufAllocator alloc, int value) {
        Optional<CachedIntegerMessage> optionalInteger = cachedIntegerMessage(value);
        if (optionalInteger.isPresent()) {
            return optionalInteger.get();
        } else {
            return DefaultIntegerMessage.create(alloc, value);
        }
    }

    public static final RespIntegerMessage integer(ByteBufAllocator alloc, long value) {
        Optional<CachedIntegerMessage> optionalInteger = cachedIntegerMessage(value);
        if (optionalInteger.isPresent()) {
            return optionalInteger.get();
        } else {
            return DefaultIntegerMessage.create(alloc, value);
        }
    }

    public static final RespErrorMessage noauth() {
        return NOAUTH;
    }

    public static final RespErrorMessage valueIsNotAnIntegerOrOutOfRange() {
        return ERR_VALUE_IS_NOT_AN_INTEGER_OR_OUT_OF_RANGE;
    }

    public static final RespErrorMessage incrementOrDecrementWouldOverflow() {
        return ERR_INCREMENT_OR_DECREMENT_WOULD_OVERFLOW;
    }

    public static final RespArrayMessage emptyArray() {
        return EMPTY_ARRAY;
    }

    public static final RespBulkStringMessage emptyBulk() {
        return EMPTY_BULK;
    }

    private RespMessages() {
    }

}
