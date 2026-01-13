package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.server.Constants.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;
import com.github.fmjsjx.libnetty.http.server.component.WebSocketSupport;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * The default implementation of {@link ChannelInitializer} for HTTP server.
 *
 * @author MJ Fang
 * @since 1.1
 */
class DefaultHttpServerChannelInitializer extends ChannelInitializer<Channel> {

    private final int timeoutSeconds;
    private final int maxContentLength;

    private final CorsConfig corsConfig;

    private final boolean sslEnabled;
    private final ChannelSslInitializer<Channel> channelSslInitializer;

    private final boolean autoCompressionEnabled;
    private final HttpContentCompressorProvider httpContentCompressorProvider;

    private final HttpServerHandlerProvider handlerProvider;

    private final HttpRequestContextDecoder contextDecoder;
    private final WebSocketInitializer webSocketInitializer;

    DefaultHttpServerChannelInitializer(int timeoutSeconds, int maxContentLength, CorsConfig corsConfig,
                                        ChannelSslInitializer<Channel> channelSslInitializer, HttpContentCompressorProvider httpContentCompressorProvider,
                                        HttpServerHandlerProvider handlerProvider, Map<Class<?>, Object> components,
                                        Consumer<HttpHeaders> addHeaders) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.corsConfig = corsConfig;
        this.sslEnabled = channelSslInitializer != null;
        this.channelSslInitializer = channelSslInitializer;
        this.autoCompressionEnabled = httpContentCompressorProvider != null;
        this.httpContentCompressorProvider = httpContentCompressorProvider;
        this.handlerProvider = handlerProvider;
        this.contextDecoder = new HttpRequestContextDecoder(components, addHeaders);
        if (components.get(WebSocketSupport.componentKey()) instanceof Optional<?> o && o.isPresent()) {
            this.webSocketInitializer = new WebSocketInitializer((WebSocketSupport) o.get());
        } else {
            this.webSocketInitializer = null;
        }
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        int timeoutSeconds = this.timeoutSeconds;
        if (timeoutSeconds > 0) {
            pipeline.addLast(TIMEOUT_HANDLER, new ReadTimeoutHandler(timeoutSeconds));
        }
        if (sslEnabled) {
            channelSslInitializer.init(ch);
        }
        pipeline.addLast(HTTP_CODEC, new HttpServerCodec());
        if (autoCompressionEnabled) {
            pipeline.addLast(HTTP_CONTENT_COMPRESSOR, httpContentCompressorProvider.create());
        }
        pipeline.addLast(HTTP_CONTENT_DECOMPRESSOR, new HttpContentDecompressor(0));
        pipeline.addLast(HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(maxContentLength));
        addWebSocketSupport(pipeline, webSocketInitializer);
        pipeline.addLast(AUTO_READ_NEXT_HANDLER, AutoReadNextHandler.getInstance());
        if (sslEnabled) {
            pipeline.addLast(HSTS_HANDLER, HstsHandler.getInstance());
        }
        if (corsConfig != null) {
            pipeline.addLast(CORS_HANDLER, new CorsHandler(corsConfig));
        }
        pipeline.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
        pipeline.addLast(HTTP_REQUEST_CONTEXT_DECODER, contextDecoder);
        pipeline.addLast(HTTP_REQUEST_CONTEXT_HANDLER, handlerProvider.get());
    }

    static void addWebSocketSupport(ChannelPipeline pipeline, WebSocketInitializer webSocketInitializer) {
        if (webSocketInitializer != null) {
            pipeline.addLast(new WebSocketServerCompressionHandler(0));
            pipeline.addLast(webSocketInitializer.createProtocolHandler());
            pipeline.addLast(webSocketInitializer);
        }
    }

    @Sharable
    static final class WebSocketInitializer extends ChannelInboundHandlerAdapter {

        private final WebSocketSupport webSocketSupport;

        WebSocketInitializer(WebSocketSupport webSocketSupport) {
            this.webSocketSupport = webSocketSupport;
        }

        WebSocketServerProtocolHandler createProtocolHandler() {
            return new WebSocketServerProtocolHandler(webSocketSupport.protocolConfig());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                var p = ctx.pipeline();
                // HttpContentDecompressor will trigger a `read` operation that is unexpected.
                // So the HttpContentDecompressor MUST be removed from channel pipeline.
                if (p.get(HTTP_CONTENT_DECOMPRESSOR) != null) {
                    p.remove(HTTP_CONTENT_DECOMPRESSOR);
                }
                // Remove other unused handlers
                if (p.get(CorsHandler.class) != null) {
                    p.remove(CorsHandler.class);
                }
                if (p.get(CHUNKED_WRITE_HANDLER) != null) {
                    p.remove(CHUNKED_WRITE_HANDLER);
                }
                // Add web socket frame handler after this handler
                p.addAfter(ctx.name(), WEB_SOCKET_FRAME_HANDLER, webSocketSupport.supplyWebSocketFrameHandler());
                // remove this handler from pipeline
                p.remove(this);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }

}
