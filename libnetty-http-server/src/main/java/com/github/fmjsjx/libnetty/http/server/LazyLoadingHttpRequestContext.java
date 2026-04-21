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
     * Waits for the HTTP post data to be completely loaded.
     *
     * @return a {@link CompletionStage} that will be completed with the
     * {@link InterfaceHttpPostRequestDecoder} when the HTTP post data is
     * completely loaded
     */
    CompletionStage<? extends InterfaceHttpPostRequestDecoder> awaitPostData();

    /**
     * Returns the {@link InterfaceHttpPostRequestDecoder} handling the
     * HTTP post data.
     *
     * @return an Optional<InterfaceHttpPostRequestDecoder>
     */
    Optional<? extends InterfaceHttpPostRequestDecoder> postData();

    /**
     * Destroys this context.
     */
    void destroy();

}
