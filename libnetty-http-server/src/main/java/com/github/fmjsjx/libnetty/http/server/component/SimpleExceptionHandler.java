package com.github.fmjsjx.libnetty.http.server.component;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.util.internal.TypeParameterMatcher;

/**
 * The abstract implementation of {@link ExceptionHandler} which allows to
 * explicit only handle a specific type of exceptions.
 *
 * @param <E> the type of exception
 */
public abstract class SimpleExceptionHandler<E extends Throwable> implements ExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleExceptionHandler.class);

    private final TypeParameterMatcher matcher;

    protected SimpleExceptionHandler() {
        matcher = TypeParameterMatcher.find(this, SimpleExceptionHandler.class, "E");
    }

    protected SimpleExceptionHandler(Class<? extends E> type) {
        matcher = TypeParameterMatcher.get(type);
    }

    /**
     * Returns {@code true} if the given {@code cause} should be handled.
     * 
     * @param cause the cause
     * @return {@code true} if the given {@code cause} should be handled
     */
    public boolean acceptCause(Throwable cause) {
        return matcher.match(cause);
    }

    @Override
    public Optional<CompletionStage<HttpResult>> handle(HttpRequestContext ctx, Throwable cause) {
        try {
            if (acceptCause(cause)) {
                @SuppressWarnings("unchecked")
                E ecause = (E) cause;
                return Optional.of(handle0(ctx, ecause));
            }
        } catch (Exception e) {
            logger.warn("Unexpected error occurs when handle {} for {}", cause, ctx, e);
        }
        return Optional.empty();
    }

    /**
     * Is called for each exception for type {@link E}.
     * 
     * @param ctx   the {@code} HttpRequestContext}
     * @param cause the cause to handle
     * @return a {@code CompletionStage<HttpResult>}
     */
    protected abstract CompletionStage<HttpResult> handle0(HttpRequestContext ctx, E cause);

}
