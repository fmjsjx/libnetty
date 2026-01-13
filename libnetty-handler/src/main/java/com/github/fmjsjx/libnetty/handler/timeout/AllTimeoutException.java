package com.github.fmjsjx.libnetty.handler.timeout;

import io.netty.channel.ChannelException;

import java.io.Serial;


/**
 * An {@link AllTimeoutException} when no data was either read or written
 * within a certain period of time.
 *
 * @author MJ Fang
 * @since 4.1
 */
public class AllTimeoutException extends ChannelException {

    @Serial
    private static final long serialVersionUID = -2452402418818743456L;

    private static final class InstanceHolder {
        private static final AllTimeoutException INSTANCE = new AllTimeoutException(true);
    }

    /**
     * Returns the singleton shared {@link AllTimeoutException} instance.
     *
     * @return an {@code AllTimeoutException} instance
     */
    public static final AllTimeoutException getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Constructs a new {@link AllTimeoutException}.
     */
    public AllTimeoutException() {
    }

    /**
     * Constructs a new {@link AllTimeoutException} with the specified
     * detail message.
     *
     * @param message the detail message.
     */
    public AllTimeoutException(String message) {
        super(message, null, false);
    }

    private AllTimeoutException(boolean shared) {
        super(null, null, shared);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
