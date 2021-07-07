package com.github.fmjsjx.libnetty.http.server.component;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

/**
 * Defines a handler to handle exceptions.
 *
 * @since 2.2
 */
public interface ExceptionHandler extends HttpServerComponent {

    @Override
    default Class<ExceptionHandler> componentType() {
        return ExceptionHandler.class;
    }

    /**
     * Handles the exception.
     * 
     * @param ctx   the {@code HttpRequestContext}
     * @param cause the cause to handle
     * @return an {@code Optional<CompletionStage<HttpResult>>}
     */
    Optional<CompletionStage<HttpResult>> handle(HttpRequestContext ctx, Throwable cause);

}
