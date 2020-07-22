package com.github.fmjsjx.libnetty.http.server;

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
    private final CorsConfig corsConfig;

    private final boolean sslEnabled;
    private final SslContextProvider sslContextProvider;

    DefaultHttpServerChannelInitializer(int timeoutSeconds, int maxContentLength, CorsConfig corsConfig,
            SslContextProvider sslContextProvider) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.corsConfig = corsConfig;
        this.sslContextProvider = sslContextProvider;
        this.sslEnabled = sslContextProvider != null;
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
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(AutoReadNextHandler.getInstance());
        pipeline.addLast(new CorsHandler(corsConfig));
        // TODO
    }

}
