package com.github.fmjsjx.libnetty.http.server;

import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * A {@link HttpRequestContext} that lazy loads the HTTP body.
 *
 * @author MJ Fang
 * @since 4.2
 */
public interface LazyLoadingHttpRequestContext extends HttpRequestContext {

    /**
     * Waits for the HTTP body to be completely loaded.
     *
     * @return a {@link CompletionStage} that will be completed with the
     * {@link InterfaceHttpPostRequestDecoder} when the HTTP body is
     * completely loaded
     */
    CompletionStage<InterfaceHttpPostRequestDecoder> awaitBodyComplete();

    /**
     * Returns the {@link InterfaceHttpPostRequestDecoder} if the HTTP body is
     * available.
     *
     * @return an Optional<InterfaceHttpPostRequestDecoder>
     */
    Optional<InterfaceHttpPostRequestDecoder> postData();

}
