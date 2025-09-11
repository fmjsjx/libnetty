package com.github.fmjsjx.libnetty.http.server;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Constants of HTTP server.
 *
 * @author MJ Fang
 * @since 3.9
 */
public final class Constants {

    /**
     * {@code "TimeoutHandler"}, the name of the timeout channel handler.
     */
    public static final String TIMEOUT_HANDLER = "TimeoutHandler";

    /**
     * {@code "HttpContentDecompressor}, the name of the decompressor
     * channel handler.
     */
    public static final String HTTP_CONTENT_DECOMPRESSOR = "HttpContentDecompressor";

    /**
     * {@code "HttpContentCompressor"}, the name of the compressor
     * channel handler.
     */
    public static final String HTTP_CONTENT_COMPRESSOR = "HttpContentCompressor";

    /**
     * {@code "HttpCodec"}, the name of the {@link HttpServerCodec}.
     */
    public static final String HTTP_CODEC = "HttpCodec";

    /**
     * {@code "HttpObjectAggregator"}, the name of the
     * {@link HttpObjectAggregator}.
     */
    public static final String HTTP_OBJECT_AGGREGATOR = "HttpObjectAggregator";

    /**
     * {@code "AutoReadNextHandler"}, the name of the auto read next
     * channel handler.
     */
    public static final String AUTO_READ_NEXT_HANDLER = "AutoReadNextHandler";

    /**
     * {@code "HstsHandler"}, the name of the {@link HstsHandler}.
     */
    public static final String HSTS_HANDLER = "HstsHandler";

    /**
     * {@code "HttpRequestContextDecoder"}, the name of the
     * {@link HttpRequestContextDecoder}.
     */
    public static final String HTTP_REQUEST_CONTEXT_DECODER = "HttpRequestContextDecoder";

    /**
     * {@code "HttpRequestContextHandler"}, the name of the
     * {@link HttpRequestContextHandler}.
     */
    public static final String HTTP_REQUEST_CONTEXT_HANDLER = "HttpRequestContextHandler";

    /**
     * {@code "ChunkedWriteHandler"}, the name of the chunked writer
     * handler.
     */
    public static final String CHUNKED_WRITE_HANDLER = "ChunkedWriteHandler";

    /**
     * {@code "WebSocketFrameHandler"}, the name of the websocket frame
     * channel handler.
     */
    public static final String WEB_SOCKET_FRAME_HANDLER = "WebSocketFrameHandler";

    /**
     * {@code "SseEventEncoder"}, the name of the SSE event encoder.
     */
    public static final String SSE_EVENT_ENCODER = "SseEventEncoder";

    private Constants() {
    }
}
