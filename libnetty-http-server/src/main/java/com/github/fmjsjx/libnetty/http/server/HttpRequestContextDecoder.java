package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.HttpCommonUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

@Sharable
class HttpRequestContextDecoder extends MessageToMessageDecoder<FullHttpRequest> {

    private final Map<Class<?>, Object> components;
    private final Consumer<HttpHeaders> addHeaders;

    HttpRequestContextDecoder(Map<Class<?>, Object> components, Consumer<HttpHeaders> addHeaders) {
        this.components = components;
        this.addHeaders = addHeaders;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest msg, List<Object> out) throws Exception {
        DecoderResult decoderResult = msg.decoderResult();
        Consumer<HttpHeaders> addHeaders = this.addHeaders;
        if (decoderResult.isFailure()) {
            HttpVersion version = msg.protocolVersion();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String text = BAD_REQUEST + " - " + decoderResult.cause();
            ByteBuf content = ctx.alloc().buffer();
            int contentLength = ByteBufUtil.writeUtf8(content, text);
            FullHttpResponse response = new DefaultFullHttpResponse(version, BAD_REQUEST, content);
            HttpHeaders headers = response.headers();
            if (addHeaders != null) {
                addHeaders.accept(headers);
            }
            HttpUtil.setKeepAlive(headers, version, keepAlive);
            headers.setInt(CONTENT_LENGTH, contentLength);
            headers.set(CONTENT_TYPE, contentType(TEXT_PLAIN, CharsetUtil.UTF_8));
            // Just respond 400 Bad Request
            ChannelFuture cf = ctx.writeAndFlush(response);
            if (!keepAlive) {
                cf.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            out.add(new DefaultHttpRequestContext(ctx.channel(), msg.retain(), components, addHeaders));
        }
    }

}
