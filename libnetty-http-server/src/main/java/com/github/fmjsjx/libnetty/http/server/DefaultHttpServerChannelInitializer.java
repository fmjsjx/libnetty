package com.github.fmjsjx.libnetty.http.server;

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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<CorsConfig> corsConfig;

    private final boolean sslEnabled;
    private final ChannelSslInitializer<Channel> channelSslInitializer;

    private final boolean autoCompressionEnabled;
    private final HttpContentCompressorProvider httpContentCompressorProvider;

    private final HttpServerHandlerProvider handlerProvider;

    private final HttpRequestContextDecoder contextDecoder;
    private final WebSocketSupport webSocketSupport;
    private final WebSocketInitializer webSocketInitializer;

    DefaultHttpServerChannelInitializer(int timeoutSeconds, int maxContentLength, CorsConfig corsConfig,
                                        ChannelSslInitializer<Channel> channelSslInitializer, HttpContentCompressorProvider httpContentCompressorProvider,
                                        HttpServerHandlerProvider handlerProvider, Map<Class<?>, Object> components,
                                        Consumer<HttpHeaders> addHeaders) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.corsConfig = Optional.ofNullable(corsConfig);
        this.sslEnabled = channelSslInitializer != null;
        this.channelSslInitializer = channelSslInitializer;
        this.autoCompressionEnabled = httpContentCompressorProvider != null;
        this.httpContentCompressorProvider = httpContentCompressorProvider;
        this.handlerProvider = handlerProvider;
        this.contextDecoder = new HttpRequestContextDecoder(components, addHeaders);
        if (components.get(WebSocketSupport.componentKey()) instanceof Optional<?> o && o.isPresent()) {
            this.webSocketInitializer = new WebSocketInitializer(this.webSocketSupport = (WebSocketSupport) o.get());
        } else {
            this.webSocketSupport = null;
            this.webSocketInitializer = null;
        }
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        int timeoutSeconds = this.timeoutSeconds;
        if (timeoutSeconds > 0) {
            pipeline.addLast("TimeoutHandler", new ReadTimeoutHandler(timeoutSeconds));
        }
        if (sslEnabled) {
            channelSslInitializer.init(ch);
        }
        pipeline.addLast("HttpCodec", new HttpServerCodec());
        if (autoCompressionEnabled) {
            pipeline.addLast("HttpContentCompressor", httpContentCompressorProvider.create());
        }
        pipeline.addLast("HttpContentDecompressor", new HttpContentDecompressor(0));
        pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(maxContentLength));
        var webSocketSupport = this.webSocketSupport;
        if (webSocketSupport != null) {
            pipeline.addLast(new WebSocketServerCompressionHandler(0));
            pipeline.addLast(new WebSocketServerProtocolHandler(webSocketSupport.protocolConfig()));
            pipeline.addLast(webSocketInitializer);
        }
        pipeline.addLast("AutoReadNextHandler", AutoReadNextHandler.getInstance());
        if (sslEnabled) {
            pipeline.addLast("HstsHandler", HstsHandler.getInstance());
        }
        corsConfig.map(CorsHandler::new).ifPresent(pipeline::addLast);
        pipeline.addLast("ChunkedWriteHandler", new ChunkedWriteHandler());
        pipeline.addLast("HttpRequestContextDecoder", contextDecoder);
        pipeline.addLast("HttpRequestContextHandler", handlerProvider.get());
    }

    @Sharable
    private static final class WebSocketInitializer extends ChannelInboundHandlerAdapter {

        private final WebSocketSupport webSocketSupport;

        private WebSocketInitializer(WebSocketSupport webSocketSupport) {
            this.webSocketSupport = webSocketSupport;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                var p = ctx.pipeline();
                // HttpContentDecompressor will trigger a `read` operation that is unexpected.
                // So the HttpContentDecompressor MUST be removed from channel pipeline.
                if (p.get("HttpContentDecompressor") != null) {
                    p.remove("HttpContentDecompressor");
                }
                // Remove other unused handlers
                if (p.get(CorsHandler.class) != null) {
                    p.remove(CorsHandler.class);
                }
                if (p.get("ChunkedWriteHandler") != null) {
                    p.remove("ChunkedWriteHandler");
                }
                // Add web socket frame handler after this handler
                p.addAfter(ctx.name(), "WebSocketFrameHandler", webSocketSupport.supplyWebSocketFrameHandler());
                // remove this handler from pipeline
                p.remove(this);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }

}
