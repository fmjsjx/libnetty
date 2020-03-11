package com.github.fmjsjx.libnetty.resp;

import static com.github.fmjsjx.libnetty.resp.CachedRespMessages.*;

import java.util.Optional;

import io.netty.buffer.ByteBufAllocator;

public class RespMessages {

    public static final RespSimpleStringMessage ok() {
        return OK;
    }

    public static final RespSimpleStringMessage pong() {
        return PONG;
    }

    public static final RespBulkStringMessage nil() {
        return NULL;
    }

    public static final RespIntegerMessage zero() {
        return ZERO;
    }

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
