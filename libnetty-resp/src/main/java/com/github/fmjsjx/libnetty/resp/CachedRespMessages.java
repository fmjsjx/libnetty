package com.github.fmjsjx.libnetty.resp;

import java.util.Optional;

import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@SuppressWarnings("unchecked")
public class CachedRespMessages {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CachedRespMessages.class);

    private static final int maxCachedIntegerLimit = 65535;

    public static final CachedErrorMessage NOAUTH = CachedErrorMessage.createAscii("NOAUTH Authentication required.");
    public static final CachedErrorMessage ERR_VALUE_IS_NOT_AN_INTEGER_OR_OUT_OF_RANGE = CachedErrorMessage
            .createErrAscii("value is not an integer or out of range");
    public static final CachedErrorMessage ERR_INCREMENT_OR_DECREMENT_WOULD_OVERFLOW = CachedErrorMessage
            .createErrAscii("increment or decrement would overflow");

    public static final CachedNullMessage NULL = CachedNullMessage.instance();

    public static final CachedBulkStringMessage EMPTY_BULK = CachedBulkStringMessage.createAscii("");

    public static final DefaultArrayMessage EMPTY_ARRAY = DefaultArrayMessage.EMPTY;

    public static final CachedSimpleStringMessage OK = CachedSimpleStringMessage.createAscii("OK");
    public static final CachedSimpleStringMessage PONG = CachedSimpleStringMessage.createAscii("PONG");

    public static final CachedIntegerMessage ZERO = CachedIntegerMessage.create(0);

    public static final CachedIntegerMessage ONE = CachedIntegerMessage.create(1);

    private static final Optional<CachedIntegerMessage>[] cachedIntegerMessages;

    static {
        int maxCachedInteger = SystemPropertyUtil.getInt("io.netty.resp.maxCachedIntegerMessage", 127);
        maxCachedInteger = Math.min(maxCachedIntegerLimit, Math.max(1, maxCachedInteger));
        logger.debug("-Dio.netty.resp.maxCachedIntegerMessage: {}", maxCachedInteger);
        cachedIntegerMessages = new Optional[maxCachedInteger + 1];
        cachedIntegerMessages[0] = Optional.of(ZERO);
        cachedIntegerMessages[1] = Optional.of(ONE);
        for (int i = 2; i < cachedIntegerMessages.length; i++) {
            cachedIntegerMessages[i] = Optional.of(CachedIntegerMessage.create(i));
        }
    }

    public static final Optional<CachedIntegerMessage> cachedIntegerMessage(long value) {
        if (value < 0 || value >= cachedIntegerMessages.length) {
            return Optional.empty();
        }
        return cachedIntegerMessages[(int) value];
    }

    public static final Optional<CachedIntegerMessage> cachedIntegerMessage(int value) {
        if (value < 0 || value >= cachedIntegerMessages.length) {
            return Optional.empty();
        }
        return cachedIntegerMessages[value];
    }

    private CachedRespMessages() {
    }

}
