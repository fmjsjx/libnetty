package com.github.fmjsjx.libnetty.http.server;

import java.util.Optional;

import com.github.fmjsjx.libnetty.http.HttpContentCompressorFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

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

    DefaultHttpServerChannelInitializer(int timeoutSeconds, int maxContentLength, CorsConfig corsConfig,
            SslContextProvider sslContextProvider, HttpContentCompressorFactory httpContentCompressorFactory) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxContentLength = maxContentLength;
        this.corsConfig = Optional.ofNullable(corsConfig);
        this.sslEnabled = sslContextProvider != null;
        this.sslContextProvider = sslContextProvider;
        this.autoCompressionEnabled = httpContentCompressorFactory != null;
        this.httpContentCompressorFactory = httpContentCompressorFactory;
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
        pipeline.addLast(new TestHandler());
        // TODO
    }

}

// TODO test code
class TestHandler extends SimpleChannelInboundHandler<HttpRequestContext> {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequestContext msg) throws Exception {
        FullHttpRequest req = msg.request();
        System.err.println("-- test --");
        System.err.println(req);
        System.err.println();
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK,
                Unpooled.copiedBuffer("OK", CharsetUtil.US_ASCII));
        res.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        HttpUtil.setContentLength(res, 2);
        HttpUtil.setKeepAlive(res, false);
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }

}
// TODO
