package com.github.fmjsjx.libnetty.resp;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Some common {@link CachedRespMessage} instances.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
@SuppressWarnings("unchecked")
public class CachedRespMessages {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CachedRespMessages.class);

    private static final int maxCachedIntegerLimit = 65535;

    /**
     * {@code -NOAUTH} Authentication required.
     */
    public static final CachedErrorMessage NOAUTH = CachedErrorMessage.createAscii("NOAUTH Authentication required.");
    /**
     * {@code -ERR} value is not an integer or out of range
     */
    public static final CachedErrorMessage ERR_VALUE_IS_NOT_AN_INTEGER_OR_OUT_OF_RANGE = CachedErrorMessage
            .createErrAscii("value is not an integer or out of range");
    /**
     * {@code -ERR} increment or decrement would overflow
     */
    public static final CachedErrorMessage ERR_INCREMENT_OR_DECREMENT_WOULD_OVERFLOW = CachedErrorMessage
            .createErrAscii("increment or decrement would overflow");

    /**
     * Cached {@code nil}.
     */
    public static final CachedNullMessage NULL = CachedNullMessage.instance();

    /**
     * Empty bulk string.
     */
    public static final CachedBulkStringMessage EMPTY_BULK = CachedBulkStringMessage.createAscii("");

    /**
     * {@code +OK}
     */
    public static final CachedSimpleStringMessage OK = CachedSimpleStringMessage.createAscii("OK");
    /**
     * {@code +PONG}
     */
    public static final CachedSimpleStringMessage PONG = CachedSimpleStringMessage.createAscii("PONG");

    /**
     * {@code :0}
     */
    public static final CachedIntegerMessage ZERO = CachedIntegerMessage.create(0);

    /**
     * {@code :1}
     */
    public static final CachedIntegerMessage ONE = CachedIntegerMessage.create(1);

    private static final Optional<CachedIntegerMessage>[] cachedIntegerMessages;

    private static final ConcurrentMap<String, CachedErrorMessage> cachedWrongNumberOfArgumentsRorCommands = new ConcurrentHashMap<>();

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

    /**
     * Returns the cached {@link CachedIntegerMessage} with the specific
     * {@code value} given if exists.
     * 
     * @param value the value as {@code long} type
     * @return an {@code Optional<CachedIntegerMessage>}
     */
    public static final Optional<CachedIntegerMessage> cachedIntegerMessage(long value) {
        if (value < 0 || value >= cachedIntegerMessages.length) {
            return Optional.empty();
        }
        return cachedIntegerMessages[(int) value];
    }

    /**
     * Returns the cached {@link CachedIntegerMessage} with the specific
     * {@code value} given if exists.
     * 
     * @param value the value as {@code int} type
     * @return an {@code Optional<CachedIntegerMessage>}
     */
    public static final Optional<CachedIntegerMessage> cachedIntegerMessage(int value) {
        if (value < 0 || value >= cachedIntegerMessages.length) {
            return Optional.empty();
        }
        return cachedIntegerMessages[value];
    }

    /**
     * Returns the cached {@link CachedErrorMessage} "wrong number of arguments for
     * '$command' command" with the specific {@code command} given
     * 
     * @param command the command text
     * @return a {@code CachedErrorMessage}
     */
    public static CachedErrorMessage cachedWrongNumberOfArgumentsForCommand(String command) {
        return cachedWrongNumberOfArgumentsRorCommands.computeIfAbsent(command,
                CachedRespMessages::createWrondNumberOfArgumentsForCommand);
    }

    private static CachedErrorMessage createWrondNumberOfArgumentsForCommand(String command) {
        return CachedErrorMessage.createErrAscii("wrong number of arguments for '" + command + "' command");
    }

    private CachedRespMessages() {
    }

}
