package com.github.fmjsjx.libnetty.http.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

class LazyLoadingHttpRequestContextDecoder extends MessageToMessageDecoder<HttpObject> {

    private static final AsciiString MULTIPART = AsciiString.cached("multipart");

    private static final FullHttpResponse TOO_LARGE_CLOSE = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER);
    private static final FullHttpResponse TOO_LARGE = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER);

    static {
        TOO_LARGE.headers().set(CONTENT_LENGTH, 0);

        TOO_LARGE_CLOSE.headers().set(CONTENT_LENGTH, 0);
        TOO_LARGE_CLOSE.headers().set(CONNECTION, HttpHeaderValues.CLOSE);
    }

    private final Map<Class<?>, Object> components;
    private final Consumer<HttpHeaders> addHeaders;
    private final boolean sslEnabled;
    private final List<HttpContent> bufferedContents;

    private LazyLoadingHttpRequestContextImpl currentCtx;
    private boolean loading;
    private long loadedLength;

    LazyLoadingHttpRequestContextDecoder(Map<Class<?>, Object> components, Consumer<HttpHeaders> addHeaders,
                                         boolean sslEnabled) {
        this.components = components;
        this.addHeaders = addHeaders;
        this.sslEnabled = sslEnabled;
        this.bufferedContents = new LinkedList<>();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!bufferedContents.isEmpty()) {
            safeReleaseAllBufferedContents();
            bufferedContents.clear();
        }
        var currentCtx = this.currentCtx;
        this.currentCtx = null;
        if (currentCtx != null) {
            var currentFuture = currentCtx.postDataFuture;
            if (currentFuture != null) {
                currentFuture.completeExceptionally(cause);
            }
            var currentDecoder = currentCtx.postDecoder;
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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (loading) {
            if (!bufferedContents.isEmpty()) {
                safeReleaseAllBufferedContents();
                bufferedContents.clear();
            }
            var currentCtx = this.currentCtx;
            this.currentCtx = null;
            var future = currentCtx.postDataFuture;
            currentCtx.postDataFuture = null;
            if (future != null) {
                future.completeExceptionally(new DecoderException("Channel closed when loading post data: " + ctx.channel()));
            }
        }
    }

    @Override
    public boolean acceptInboundMessage(Object msg) {
        if (loading) {
            return msg instanceof HttpContent;
        }
        if (msg instanceof HttpRequest req && !(req instanceof FullHttpRequest)) {
            var mimeType = HttpUtil.getMimeType(req);
            return isMultipart(mimeType);
        }
        return false;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) {
        if (msg instanceof HttpRequest request) {
            // is multipart request
            var fullRequest = new DefaultFullHttpRequest(request.protocolVersion(), request.method(),
                    request.uri(), Unpooled.EMPTY_BUFFER, request.headers(), trailersFactory().newHeaders());
            currentCtx = new LazyLoadingHttpRequestContextImpl(request, ctx.channel(), fullRequest, this, components, addHeaders, sslEnabled);
            loading = true;
            loadedLength = 0;
            if (!bufferedContents.isEmpty()) {
                safeReleaseAllBufferedContents();
                bufferedContents.clear();
            }
            out.add(currentCtx);
            return;
        }
        if (loading && msg instanceof HttpContent chunk) {
            var currentCtx = this.currentCtx;
            var future = currentCtx.postDataFuture;
            var maxContentLength = currentCtx.maxContentLength;
            if (maxContentLength >= 0 && chunk.content().readableBytes() + loadedLength > maxContentLength) {
                currentCtx.postDataFuture = null;
                this.currentCtx = null;
                loading = false;
                loadedLength = 0;
                future.completeExceptionally(new TooLongFrameException("content length exceeded " + maxContentLength + " bytes."));
                closeChannelAsync(ctx);
                return;
            }
            var postDecoder = currentCtx.postDecoder;
            if (postDecoder == null) {
                loadedLength += chunk.content().readableBytes();
                bufferedContents.add(chunk.retain());
                return;
            }
            if (!bufferedContents.isEmpty()) {
                try {
                    for (var buffered : bufferedContents) {
                        postDecoder.offer(buffered);
                    }
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                    closeChannelAsync(ctx);
                    return;
                } finally {
                    safeReleaseAllBufferedContents();
                    bufferedContents.clear();
                }
            }
            if (chunk instanceof LastHttpContent lastChunk) {
                this.currentCtx = null;
                loading = false;
                loadedLength = 0;
                try {
                    postDecoder.offer(lastChunk);
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                    closeChannelAsync(ctx);
                    return;
                }
                if (!lastChunk.trailingHeaders().isEmpty()) {
                    currentCtx.request().trailingHeaders().setAll(lastChunk.trailingHeaders());
                }
                currentCtx.postDataFuture = null;
                future.complete(postDecoder);
                return;
            }
            try {
                postDecoder.offer(chunk);
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
                closeChannelAsync(ctx);
                return;
            }
            ctx.read();
        }
    }

    private void safeReleaseAllBufferedContents() {
        for (var buffered : bufferedContents) {
            ReferenceCountUtil.safeRelease(buffered);
        }
    }

    private void closeChannelAsync(ChannelHandlerContext ctx) {
        closeChannelAsync(ctx.channel());
    }

    private void closeChannelAsync(Channel channel) {
        if (!channel.isActive()) {
            return;
        }
        // We should close the channel in even loop because the business layer may not close the channel.
        channel.eventLoop().execute(() -> {
            if (channel.isActive()) {
                channel.close();
            }
        });
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
        private final LazyLoadingHttpRequestContextDecoder decoder;
        private HttpPostMultipartRequestDecoder postDecoder;
        private CompletableFuture<HttpPostMultipartRequestDecoder> postDataFuture;
        private long maxContentLength = -1;

        private LazyLoadingHttpRequestContextImpl(HttpRequest currentRequest, Channel channel, FullHttpRequest request,
                                                  LazyLoadingHttpRequestContextDecoder decoder,
                                                  Map<Class<?>, Object> components, Consumer<HttpHeaders> addHeaders,
                                                  boolean sslEnabled) {
            super(channel, request, components, addHeaders, sslEnabled);
            this.currentRequest = currentRequest;
            this.decoder = decoder;
        }

        @Override
        public CompletionStage<? extends InterfaceHttpPostRequestDecoder> awaitPostData(long maxContentLength) {
            this.maxContentLength = maxContentLength;
            var req = currentRequest;
            var postDecoder = this.postDecoder = new HttpPostMultipartRequestDecoder(req);
            var future = postDataFuture = new CompletableFuture<>();
            var decoder = this.decoder;
            if (!decoder.bufferedContents.isEmpty()) {
                var lastChunk = decoder.bufferedContents.getLast();
                if (lastChunk instanceof LastHttpContent lastContent) {
                    try {
                        for (var buffered : decoder.bufferedContents) {
                            postDecoder.offer(buffered);
                        }
                        future.complete(postDecoder);
                        decoder.currentCtx = null;
                        decoder.loading = false;
                        decoder.loadedLength = 0;
                        if (!lastContent.trailingHeaders().isEmpty()) {
                            request().trailingHeaders().setAll(lastContent.trailingHeaders());
                        }
                        postDataFuture = null;
                    } catch (RuntimeException e) {
                        future.completeExceptionally(e);
                        decoder.closeChannelAsync(channel());
                    } finally {
                        decoder.safeReleaseAllBufferedContents();
                        decoder.bufferedContents.clear();
                    }
                    return future;
                }
            }
            if (HttpUtil.is100ContinueExpected(req)) {
                HttpResponse accept = ACCEPT.retainedDuplicate();
                channel().writeAndFlush(accept).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
            channel().read();
            return future;
        }

        @Override
        public Optional<? extends InterfaceHttpPostRequestDecoder> postData() {
            return Optional.ofNullable(postDecoder);
        }

        @Override
        public void destroy() {
            var decoder = this.postDecoder;
            if (decoder != null) {
                this.postDecoder = null;
                try {
                    decoder.destroy();
                } catch (Exception e) {
                    // ignore error here
                }
            }
        }

    }

}
