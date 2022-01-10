package com.github.fmjsjx.libnetty.http.server;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * The default implementation of {@link ChannelInitializer} for HTTP server.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
class DefaultHttpServerChannelInitializer extends ChannelInitializer<Channel> {

    private final int timeoutSeconds;
    private final int maxContentLength;

    private final Optional<CorsConfig> corsConfig;

    private final boolean sslEnabled;
    private final ChannelSslInitializer<Channel> channelSslInitializer;

    private final boolean autoCompressionEnabled;
    private final HttpContentCompressorProvider httpContentCompressorProvider;

    private final HttpServerHandlerProvider handlerProvider;

    private final HttpRequestContextDecoder contextDecoder;

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
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        int timeoutSeconds = this.timeoutSeconds;
        if (timeoutSeconds > 0) {
            pipeline.addLast(new ReadTimeoutHandler(timeoutSeconds));
        }
        if (sslEnabled) {
            channelSslInitializer.init(ch);
        }
        pipeline.addLast(new HttpServerCodec());
        if (autoCompressionEnabled) {
            pipeline.addLast(httpContentCompressorProvider.create());
        }
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(AutoReadNextHandler.getInstance());
        if (sslEnabled) {
            pipeline.addLast(HstsHandler.getInstance());
        }
        corsConfig.map(CorsHandler::new).ifPresent(pipeline::addLast);
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(contextDecoder);
        pipeline.addLast(handlerProvider.get());
    }

}
