package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.server.Constants.*;
import static com.github.fmjsjx.libnetty.http.server.DefaultHttpServerChannelInitializer.addWebSocketSupport;
import static com.github.fmjsjx.libnetty.http.server.DefaultHttpServerHandlerProvider.DEFAULT_EXCEPTION_HANDLER;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.handler.timeout.AllTimeoutHandler;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;
import com.github.fmjsjx.libnetty.http.server.component.WebSocketSupport;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServerChannelInitializer.WebSocketInitializer;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


class DefaultHttp2ServerChannelInitializer extends ChannelInitializer<Channel> {

    private static final Logger log = LoggerFactory.getLogger(DefaultHttp2ServerChannelInitializer.class);

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

    private final UpgradeCodecFactory upgradeCodecFactory;
    private final Http2StreamInitializer http2StreamInitializer;
    private final UpgradeEventHandler upgradeEventHandler;
    private final HttpMessageHandler httpMessageHandler;
    private final Http2ParentChannelExceptionHandler http2ParentChannelExceptionHandler;

    DefaultHttp2ServerChannelInitializer(int timeoutSeconds, int maxContentLength, CorsConfig corsConfig,
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
        this.contextDecoder = new HttpRequestContextDecoder(components, addHeaders, sslEnabled);
        if (components.get(WebSocketSupport.componentKey()) instanceof Optional<?> o && o.isPresent()) {
            this.webSocketInitializer = new WebSocketInitializer((WebSocketSupport) o.get());
        } else {
            this.webSocketInitializer = null;
        }
        http2StreamInitializer = new Http2StreamInitializer();
        upgradeEventHandler = new UpgradeEventHandler();
        if (handlerProvider instanceof DefaultHttpServerHandlerProvider defaultHandlerProvider) {
            http2ParentChannelExceptionHandler = new Http2ParentChannelExceptionHandler(defaultHandlerProvider.getExceptionHandler());
        } else {
            http2ParentChannelExceptionHandler = new Http2ParentChannelExceptionHandler(DEFAULT_EXCEPTION_HANDLER);
        }
        if (sslEnabled) {
            upgradeCodecFactory = null;
            httpMessageHandler = null;
        } else {
            upgradeCodecFactory = createUpgradeCodecFactory();
            httpMessageHandler = new HttpMessageHandler();
        }
    }

    private UpgradeCodecFactory createUpgradeCodecFactory() {
        return protocol -> {
            System.err.println("upgradeCodecFactory: " + protocol);
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(Http2FrameCodecBuilder.forServer().build(),
                        new Http2MultiplexHandler(http2StreamInitializer),
                        http2ParentChannelExceptionHandler);
            }
            return null;
        };
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        int timeoutSeconds = this.timeoutSeconds;
        if (timeoutSeconds > 0) {
            ch.pipeline().addLast(TIMEOUT_HANDLER, new AllTimeoutHandler(timeoutSeconds));
        }
        if (sslEnabled) {
            configureSsl(ch);
        } else {
            configureClearText(ch);
        }
    }

    private void configureSsl(Channel ch) throws Exception {
        var pipeline = ch.pipeline();
        channelSslInitializer.init(ch);
        pipeline.addLast(new Http2OrHttpHandler());
    }

    private class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

        private Http2OrHttpHandler() {
            super(ApplicationProtocolNames.HTTP_1_1);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.read();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
            log.warn("configurePipeline: {} <- {}", protocol, ctx.channel());
            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                configureHttp2(ctx);
                return;
            } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                configureAlpnNegotiation(ctx);
                return;
            }
            throw new IllegalStateException("unknown protocol: " + protocol);
        }

        private void configureHttp2(ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().build(),
                    new Http2MultiplexHandler(http2StreamInitializer),
                    http2ParentChannelExceptionHandler);
            ensureAutoRead(ctx);
        }

        private void configureAlpnNegotiation(ChannelHandlerContext ctx) {
            var pipeline = ctx.pipeline();
            pipeline.addLast(HTTP_CODEC, new HttpServerCodec());
            if (autoCompressionEnabled) {
                pipeline.addLast(HTTP_CONTENT_COMPRESSOR, httpContentCompressorProvider.create());
            }
            pipeline.addLast(HTTP_CONTENT_DECOMPRESSOR, new HttpContentDecompressor(0));
            pipeline.addLast(HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(maxContentLength));
            addWebSocketSupport(pipeline, webSocketInitializer);
            pipeline.addLast(AUTO_READ_NEXT_HANDLER, AutoReadNextHandler.getInstance());
            pipeline.addLast(HSTS_HANDLER, HstsHandler.getInstance());
            if (corsConfig != null) {
                pipeline.addLast(CORS_HANDLER, new CorsHandler(corsConfig));
            }
            pipeline.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
            pipeline.addLast(HTTP_REQUEST_CONTEXT_DECODER, contextDecoder);
            pipeline.addLast(HTTP_REQUEST_CONTEXT_HANDLER, handlerProvider.get());
        }

    }

    private static void ensureAutoRead(ChannelHandlerContext ctx) {
        if (!ctx.channel().config().isAutoRead()) {
            ctx.channel().config().setAutoRead(true);
            ctx.read();
        }
    }

    private void configureClearText(Channel ch) {
        var pipeline = ch.pipeline();
        var sourceCodec = new HttpServerCodec();
        pipeline.addLast(HTTP_CODEC, sourceCodec);
        pipeline.addLast(new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory));
        pipeline.addLast(httpMessageHandler);
        pipeline.addLast(upgradeEventHandler);
    }

    @Sharable
    private class HttpMessageHandler extends SimpleChannelInboundHandler<HttpMessage> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.read();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) {
            // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
            var pipeline = ctx.pipeline();
            if (autoCompressionEnabled) {
                pipeline.addAfter(ctx.name(), HTTP_CONTENT_COMPRESSOR, httpContentCompressorProvider.create());
            }
            pipeline.replace(this, HTTP_CONTENT_DECOMPRESSOR, new HttpContentDecompressor(0));
            pipeline.addLast(HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(maxContentLength));
            addWebSocketSupport(pipeline, webSocketInitializer);
            pipeline.addLast(AUTO_READ_NEXT_HANDLER, AutoReadNextHandler.getInstance());
            if (corsConfig != null) {
                pipeline.addLast(CORS_HANDLER, new CorsHandler(corsConfig));
            }
            pipeline.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
            pipeline.addLast(HTTP_REQUEST_CONTEXT_DECODER, contextDecoder);
            pipeline.addLast(HTTP_REQUEST_CONTEXT_HANDLER, handlerProvider.get());
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }

    }

    @Sharable
    private class Http2StreamInitializer extends ChannelInitializer<Http2StreamChannel> {

        @Override
        protected void initChannel(Http2StreamChannel ch) {
            var pipeline = ch.pipeline();

            pipeline.addLast(new Http2StreamFrameToHttpObjectCodec(true));
            pipeline.addLast(HTTP_CONTENT_DECOMPRESSOR, new HttpContentDecompressor(0));
            if (autoCompressionEnabled) {
                pipeline.addLast(HTTP_CONTENT_COMPRESSOR, httpContentCompressorProvider.create());
            }
            pipeline.addLast(HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(maxContentLength));
            if (corsConfig != null) {
                pipeline.addLast(CORS_HANDLER, new CorsHandler(corsConfig));
            }
            pipeline.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
            pipeline.addLast(HTTP_REQUEST_CONTEXT_DECODER, contextDecoder);
            pipeline.addLast(HTTP_REQUEST_CONTEXT_HANDLER, handlerProvider.get());
        }

    }

    @Sharable
    private static class UpgradeEventHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
                log.debug("Upgrade Event triggered: {}", evt);
                ensureAutoRead(ctx);
            }
            ctx.fireUserEventTriggered(evt);
        }

    }

    @Sharable
    private static class Http2ParentChannelExceptionHandler extends ChannelInboundHandlerAdapter {

        private final BiConsumer<ChannelHandlerContext, Throwable> exceptionHandler;

        private Http2ParentChannelExceptionHandler(BiConsumer<ChannelHandlerContext, Throwable> exceptionHandler) {
            this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler must not be null");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            exceptionHandler.accept(ctx, cause);
        }

    }

}
