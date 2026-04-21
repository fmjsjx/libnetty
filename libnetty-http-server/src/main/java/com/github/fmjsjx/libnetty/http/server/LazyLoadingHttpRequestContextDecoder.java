package com.github.fmjsjx.libnetty.http.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.AsciiString;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

class LazyLoadingHttpRequestContextDecoder extends MessageToMessageDecoder<HttpObject> {

    private static final AsciiString MULTIPART = AsciiString.cached("multipart");

    private final Map<Class<?>, Object> components;
    private final Consumer<HttpHeaders> addHeaders;
    private final boolean sslEnabled;

    private LazyLoadingHttpRequestContextImpl currentCtx;
    private boolean loading;

    LazyLoadingHttpRequestContextDecoder(Map<Class<?>, Object> components, Consumer<HttpHeaders> addHeaders,
                                         boolean sslEnabled) {
        this.components = components;
        this.addHeaders = addHeaders;
        this.sslEnabled = sslEnabled;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        var currentCtx = this.currentCtx;
        if (currentCtx != null) {
            var currentFuture = currentCtx.postDataFuture;
            if (currentFuture != null) {
                currentFuture.completeExceptionally(cause);
            }
            var currentDecoder = currentCtx.decoder;
            if (currentDecoder != null) {
                try {
                    currentDecoder.destroy();
                } catch (Exception e) {
                    // ignore error here
                }
            }
        }
        ctx.fireExceptionCaught(cause);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) {
        if (msg instanceof HttpRequest request) {
            var mimeType = HttpUtil.getMimeType(request);
            if (isMultipart(mimeType)) {
                var fullRequest = new DefaultFullHttpRequest(request.protocolVersion(), request.method(),
                        request.uri(), Unpooled.EMPTY_BUFFER, request.headers(), trailersFactory().newHeaders());
                currentCtx = new LazyLoadingHttpRequestContextImpl(request, ctx.channel(), fullRequest, components, addHeaders, sslEnabled);
                loading = true;
                out.add(currentCtx);
            } else {
                out.add(request);
            }
            return;
        }
        if (loading && msg instanceof HttpContent chunk) {
            if (chunk instanceof LastHttpContent lastChunk) {
                var currentCtx = this.currentCtx;
                this.currentCtx = null;
                loading = false;
                var decoder = currentCtx.decoder;
                decoder.offer(lastChunk);
                if (!lastChunk.trailingHeaders().isEmpty()) {
                    currentCtx.request().trailingHeaders().setAll(lastChunk.trailingHeaders());
                }
                var future = currentCtx.postDataFuture;
                currentCtx.postDataFuture = null;
                future.complete(decoder);
                return;
            }
            currentCtx.decoder.offer(chunk);
            ctx.read();
            return;
        }
        out.add(msg);
    }

    private boolean isMultipart(CharSequence mimeType) {
        if (mimeType == null) {
            return false;
        }
        if (mimeType instanceof AsciiString ascii) {
            return ascii.startsWith(MULTIPART);
        }
        return mimeType.toString().startsWith(MULTIPART.toString());
    }

    private static final class LazyLoadingHttpRequestContextImpl extends DefaultHttpRequestContext
            implements LazyLoadingHttpRequestContext {

        private static final FullHttpResponse ACCEPT = new DefaultFullHttpResponse(
                HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER);

        static {
            ACCEPT.headers().set(CONTENT_LENGTH, 0);
        }

        private final HttpRequest currentRequest;
        private HttpPostMultipartRequestDecoder decoder;
        private CompletableFuture<HttpPostMultipartRequestDecoder> postDataFuture;

        private LazyLoadingHttpRequestContextImpl(HttpRequest currentRequest, Channel channel, FullHttpRequest request,
                                                  Map<Class<?>, Object> components, Consumer<HttpHeaders> addHeaders,
                                                  boolean sslEnabled) {
            super(channel, request, components, addHeaders, sslEnabled);
            this.currentRequest = currentRequest;
        }

        @Override
        public CompletionStage<? extends InterfaceHttpPostRequestDecoder> awaitPostData() {
            var req = currentRequest;
            decoder = new HttpPostMultipartRequestDecoder(req);
            postDataFuture = new CompletableFuture<>();
            if (HttpUtil.is100ContinueExpected(req)) {
                HttpResponse accept = ACCEPT.retainedDuplicate();
                channel().writeAndFlush(accept).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
            channel().read();
            return postDataFuture;
        }

        @Override
        public Optional<? extends InterfaceHttpPostRequestDecoder> postData() {
            return Optional.ofNullable(decoder);
        }

        @Override
        public void destroy() {
            var decoder = this.decoder;
            if (decoder != null) {
                this.decoder = null;
                try {
                    decoder.destroy();
                } catch (Exception e) {
                    // ignore error here
                }
            }
        }

    }

}
