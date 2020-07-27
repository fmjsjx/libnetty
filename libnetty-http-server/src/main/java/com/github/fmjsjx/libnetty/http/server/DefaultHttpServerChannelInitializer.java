package com.github.fmjsjx.libnetty.http.server;

import java.util.Optional;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
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
    private final SslContextProvider sslContextProvider;

    private final boolean autoCompressionEnabled;
    private final HttpContentCompressorFactory httpContentCompressorFactory;

    private final HttpServerHandlerProvider handlerProvider;

    DefaultHttpServerChannelInitializer(int timeoutSeconds, int maxContentLength, CorsConfig corsConfig,
            SslContextProvider sslContextProvider, HttpContentCompressorFactory httpContentCompressorFactory,
            HttpServerHandlerProvider handlerProvider) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.corsConfig = Optional.ofNullable(corsConfig);
        this.sslEnabled = sslContextProvider != null;
        this.sslContextProvider = sslContextProvider;
        this.autoCompressionEnabled = httpContentCompressorFactory != null;
        this.httpContentCompressorFactory = httpContentCompressorFactory;
        this.handlerProvider = handlerProvider;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new ReadTimeoutHandler(timeoutSeconds));
        if (sslEnabled) {
            SslContext sslContext = sslContextProvider.get();
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        if (autoCompressionEnabled) {
            pipeline.addLast(httpContentCompressorFactory.create());
        }
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(AutoReadNextHandler.getInstance());
        corsConfig.map(CorsHandler::new).ifPresent(pipeline::addLast);
        pipeline.addLast(HttpRequestContextDecoder.getInstance());
        pipeline.addLast(handlerProvider.get());
    }

}