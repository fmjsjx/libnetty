package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * A {@link Middleware} validates HTTP requests.
 * 
 * @since 1.2
 *
 * @author MJ Fang
 */
public abstract class RequestValidator implements Middleware {

    /**
     * Returns a new {@link RequestValidator} instance using the specified
     * validation function.
     * 
     * @param validation the validation function
     * @return a {@code RequestValidator} instance
     */
    public static final RequestValidator simple(Predicate<HttpRequestContext> validation) {
        return new SimpleRequestValidator(validation);
    }

    /**
     * Returns a new {@link RequestValidator} instance using the specified
     * validation function.
     * 
     * @param validation    the validation function
     * @param failureStatus the failure status
     * @return a {@code RequestValidator} instance
     */
    public static final RequestValidator simple(Predicate<HttpRequestContext> validation,
            HttpResponseStatus failureStatus) {
        return new SimpleRequestValidator(validation, failureStatus);
    }

    private static final HttpResponseStatus DEFAULT_FAILURE_STATUS = HttpResponseStatus.FORBIDDEN;

    private final HttpResponseStatus failureStatus;

    protected RequestValidator() {
        this(DEFAULT_FAILURE_STATUS);
    }

    protected RequestValidator(HttpResponseStatus failureStatus) {
        this.failureStatus = Objects.requireNonNull(failureStatus, "failureStatus must not be null");
    }

    /**
     * Returns the status when validation failed.
     * 
     * @return the status when validation failed
     */
    public final HttpResponseStatus failureStatus() {
        return failureStatus;
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        try {
            if (validate(ctx)) {
                return next.doNext(ctx);
            }
            return ctx.simpleRespond(failureStatus());
        } catch (Exception e) {
            return ctx.respondError(e);
        }
    }

    /**
     * Validate the {@link HttpRequestContext}.
     * <p>
     * Returns {@code true} if the context is valid, {@code false} otherwise.
     * 
     * @param ctx the HTTP request context
     * @return {@code true} if the context is valid, {@code false} otherwise
     * 
     * @throws Exception if any error occurs
     */
    protected abstract boolean validate(HttpRequestContext ctx);

    private static final class SimpleRequestValidator extends RequestValidator {

        private final Predicate<HttpRequestContext> validation;

        private SimpleRequestValidator(Predicate<HttpRequestContext> validation) {
            this.validation = Objects.requireNonNull(validation, "validation must not be null");
        }

        private SimpleRequestValidator(Predicate<HttpRequestContext> validation, HttpResponseStatus failureStatus) {
            super(failureStatus);
            this.validation = Objects.requireNonNull(validation, "validation must not be null");
        }

        @Override
        protected boolean validate(HttpRequestContext ctx) {
            return validation.test(ctx);
        }

    }

}
