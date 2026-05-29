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
    default CompletionStage<? extends InterfaceHttpPostRequestDecoder> awaitPostData() {
        return awaitPostData(-1);
    }

    /**
     * Waits for the HTTP post data to be completely loaded.
     *
     * @param maxContentLength the maximum length of the content in bytes
     * @return a {@link CompletionStage} that will be completed with the
     * {@link InterfaceHttpPostRequestDecoder} when the HTTP post data is
     * completely loaded
     */
    CompletionStage<? extends InterfaceHttpPostRequestDecoder> awaitPostData(long maxContentLength);

    /**
     * Returns the {@link InterfaceHttpPostRequestDecoder} handling the
     * HTTP post data.
     *
     * @return an {@code Optional<InterfaceHttpPostRequestDecoder>}
     */
    Optional<? extends InterfaceHttpPostRequestDecoder> postData();

    /**
     * Destroys this context.
     */
    void destroy();

}
