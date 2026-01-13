package com.github.fmjsjx.libnetty.example.http.server;

import static com.github.fmjsjx.libnetty.http.HttpCommonUtil.contentType;
import static com.github.fmjsjx.libnetty.http.server.Constants.TIMEOUT_HANDLER;
import static com.github.fmjsjx.libnetty.http.server.HttpServerHandler.READ_NEXT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;
import static io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType.FileUpload;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fmjsjx.libcommon.json.Fastjson2Library;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.annotation.HeaderValue;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpGet;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPost;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.QueryVar;
import com.github.fmjsjx.libnetty.http.server.annotation.RemoteAddr;
import com.github.fmjsjx.libnetty.http.server.annotation.StringBody;
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException;

import com.github.fmjsjx.libnetty.http.server.exception.SimpleHttpFailureException;
import com.github.fmjsjx.libnetty.http.server.sse.SseEventBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * Test controller
 */
@SuppressWarnings({"DuplicatedCode", "OptionalUsedAsFieldOrParameterType"})
@HttpPath("/api")
@HttpPath("/api_dup")
public class TestController {

    static final AsciiString ASCII_OK = AsciiString.cached("OK");

    /**
     * Constructs a new {@link TestController} instance.
     */
    public TestController() {
    }

    /**
     * GET /api/test
     *
     * @param ctx request context
     * @return result
     */
    @HttpGet("/test")
    public CompletionStage<HttpResult> getTest(HttpRequestContext ctx) {
        // GET /api/test
        System.out.println("-- test --");
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
        System.out.println(body.toString(CharsetUtil.UTF_8));
        return ctx.simpleRespond(OK, body, TEXT_PLAIN);
    }

    /**
     * GET /api/errors/{code}
     *
     * @param ctx       http request context
     * @param code      code
     * @param clientIp  client IP
     * @param userAgent user-agent in header
     * @return result
     */
    @HttpGet("/errors/{code}")
    public CompletionStage<HttpResult> getErrors(
            HttpRequestContext ctx, @PathVar("code") int code, @RemoteAddr String clientIp,
            @HeaderValue("user-agent") Optional<String> userAgent) {
        // GET /api/errors/{code}
        System.out.println("-- errors --");
        System.out.println("client IP ==> " + clientIp);
        System.out.println("user agent ==> " + userAgent);
        HttpResponseStatus status = HttpResponseStatus.valueOf(code);
        System.out.println("status ==> " + status);
        return ctx.simpleRespond(status);
    }

    /**
     * GET /api/jsons
     *
     * @param query     query
     * @param eventLoop current eventLoop
     * @return result
     */
    @HttpGet("/jsons")
    @JsonBody
    public CompletableFuture<?> getJsons(QueryStringDecoder query, EventLoop eventLoop) {
        // GET /api/jsons
        System.out.println("-- jsons --");
        //        System.out.println("library: " + library);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        query.parameters().forEach((key, values) -> {
            if (values.size() == 1) {
                node.put(key, values.getFirst());
            } else {
                node.putPOJO(key, values);
            }
        });
        return CompletableFuture.supplyAsync(() -> {
            if (node.isEmpty()) {
                throw new ManualHttpFailureException(BAD_REQUEST, "{\"code\":1,\"message\":\"Missing Query String\"}",
                        HttpHeaderValues.APPLICATION_JSON, "Missing Query String");
            } else {
                return node;
            }
        }, eventLoop);
    }

    /**
     * POST /api/jsons/form
     *
     * @param ctx http request context
     * @return result
     * @throws Exception any error occurs
     */
    @HttpPost("/jsons/form")
    @JsonBody
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletionStage<?> postJsonsForm(HttpRequestContext ctx) throws Exception {
        System.out.println("-- jsons form --");
        var result = new LinkedHashMap<String, Object>();
        var decoder = new HttpPostRequestDecoder(ctx.request());
        try {
            for (var ihd : decoder.getBodyHttpDatas()) {
                if (ihd instanceof Attribute attr) {
                    var val = result.get(attr.getName());
                    if (val == null) {
                        result.put(attr.getName(), attr.getValue());
                    } else {
                        if (val instanceof List list) {
                            list.add(attr.getValue());
                        } else {
                            var list = new ArrayList<>();
                            list.add(val);
                            list.add(attr.getValue());
                            result.put(attr.getName(), list);
                        }
                    }
                }
            }
            return CompletableFuture.completedStage(result);
        } finally {
            decoder.destroy();
        }
    }

    /**
     * POST /api/echo
     *
     * @param ctx   http request context
     * @param value json body value
     * @return result
     */
    @HttpPost("/echo")
    public CompletionStage<HttpResult> postEcho(HttpRequestContext ctx, @JsonBody JsonNode value) {
        // POST /api/echo
        System.out.println("-- echo --");
        System.out.println("value ==> " + value);
        ByteBuf content = ctx.request().content();
        Charset charset = HttpUtil.getCharset(ctx.request(), CharsetUtil.UTF_8);
        CharSequence contentType = ctx.contentType().orElseGet(() -> contentType(TEXT_PLAIN, charset));
        return ctx.simpleRespond(OK, content.retain(), contentType);
    }

    /**
     * GET /api/no-content
     *
     * @param query    query
     * @param executor current executor (eventLoop actually)
     * @return result
     */
    @HttpGet("/no-content")
    public CompletionStage<Void> getNoContent(QueryStringDecoder query, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            // GET /api/no-content
            System.out.println("-- no content --");
            System.out.println(query.uri());
        }, executor);
    }

    /**
     * GET /api/ok
     *
     * @param ctx   the {@link HttpRequestContext}
     * @param query query
     * @return result
     */
    @HttpGet("/ok")
    @StringBody
    public CompletionStage<CharSequence> getOK(HttpRequestContext ctx, QueryStringDecoder query) {
        System.out.println("-- ok --");
        System.out.println(ctx.channel());
        System.out.println(query.uri());
        return CompletableFuture.completedFuture(ASCII_OK);
    }

    /**
     * GET /api/error
     *
     * @param test     query parameter test
     * @param executor current executor (eventLoop actually)
     * @return result
     */
    @HttpGet("/error")
    public CompletionStage<Void> getError(@QueryVar("test") OptionalInt test, Executor executor) {
        System.err.println("-- error --");
        System.err.println(test);
        return CompletableFuture
                .failedStage(test.orElse(0) == 1 ? new TestException("test error") : new Exception("no test"));
    }

    /**
     * POST /api/upload
     *
     * @param ctx http request context
     * @return result
     * @throws Exception any error occurs
     */
    @HttpPost("/upload")
    @StringBody
    public CompletionStage<CharSequence> postUpload(HttpRequestContext ctx) throws Exception {
        var decoder = new HttpPostRequestDecoder(ctx.request());
        try {
            if (!decoder.isMultipart()) {
                throw new SimpleHttpFailureException(BAD_REQUEST, "post body content type must be multipart/form-data");
            }
            var fileUpload = decoder.getBodyHttpData("file");
            if (fileUpload == null || fileUpload.getHttpDataType() != FileUpload) {
                throw new SimpleHttpFailureException(BAD_REQUEST, "invalid file");
            }
            if (fileUpload instanceof FileUpload file) {
                var dfile = new File(file.getFilename());
                file.renameTo(dfile);
                System.out.println("-- file --");
                System.out.println(dfile);
            }
            return CompletableFuture.completedFuture(ASCII_OK);
        } finally {
            decoder.destroy();
        }
    }

    /**
     * GET /api/array
     *
     * @param ctx   request context
     * @param names names
     * @param ids   ids
     * @return result
     */
    @HttpGet("/array")
    public CompletionStage<HttpResult> getArray(HttpRequestContext ctx, @QueryVar("name[]") List<String> names, @QueryVar("id[]") int[] ids) {
        // GET /api/test
        System.out.println("-- array --");
        System.out.println("query --- " + ctx.rawQuery());
        System.out.println("names --- " + names);
        System.out.println("ids --- " + Arrays.toString(ids));
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
        System.out.println(body.toString(CharsetUtil.UTF_8));
        return ctx.simpleRespond(OK, body, TEXT_PLAIN);
    }

    /**
     * GET /api/test/event-stream
     * <p>
     * A demo API for the low level {@code SSE} implementation.
     *
     * @param ctx request context
     * @param len the length of the event message
     * @return result
     * @since 3.9
     */
    @HttpGet("/test/event-stream")
    public CompletionStage<HttpResult> getTestEventStream(HttpRequestContext ctx, @QueryVar(value = "len", required = false) Integer len) {
        // GET /api/test/event-stream
        System.out.println("-- test event-stream --");
        System.out.println(ctx.channel());
        int messageSize = len == null ? 100 : len;
        var channel = ctx.channel();
        var pipeline = channel.pipeline();
        var timeoutHandler = pipeline.get(TIMEOUT_HANDLER);
        if (timeoutHandler instanceof ReadTimeoutHandler) {
            pipeline.remove(TIMEOUT_HANDLER);
        }
        var response = ctx.responseFactory().create(OK);
        HttpUtil.setTransferEncodingChunked(response, true);
        response.headers().set(CONTENT_TYPE, TEXT_EVENT_STREAM);
        response.headers().set(CONTENT_ENCODING, IDENTITY);
        var future = new CompletableFuture<HttpResult>();
        channel.writeAndFlush(response).addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, -1, OK));

            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });

        var eventContent = channel.alloc().buffer();
        eventContent.writeBytes("event: ping\n\n".getBytes());
        channel.writeAndFlush(eventContent);
        // id: 1\n\n
        channel.writeAndFlush(channel.alloc().buffer().writeBytes("id: 1\n\n".getBytes()));
        var writeStreamTask = new Runnable() {

            private int n;

            @Override
            public void run() {
                if (n++ < messageSize) {
                    var content = channel.alloc().buffer();
                    // {"line":}
                    var data = "data: {\"line\":" + n + "}\n\n";
                    content.writeBytes(data.getBytes(CharsetUtil.UTF_8));
                    channel.writeAndFlush(content);
                    channel.eventLoop().schedule(this, 1000, TimeUnit.MILLISECONDS);
                } else {
                    var content = channel.alloc().buffer();
                    // {"line":}
                    var data = "event: close\ndata: {}\n\n";
                    content.writeBytes(data.getBytes(CharsetUtil.UTF_8));
                    channel.writeAndFlush(content);
                    channel.writeAndFlush(EMPTY_LAST_CONTENT).addListener(READ_NEXT);
                    if (timeoutHandler instanceof ReadTimeoutHandler readTimeoutHandler) {
                        channel.pipeline().addFirst(TIMEOUT_HANDLER, new ReadTimeoutHandler(readTimeoutHandler.getReaderIdleTimeInMillis(), TimeUnit.MILLISECONDS));
                    }
                }
            }
        };
        channel.eventLoop().schedule(writeStreamTask, 1000, TimeUnit.MILLISECONDS);
        return future;
    }

    static final AsciiString SSE_EVENT_CLOSE = AsciiString.cached("close");
    static final AsciiString SSE_EVENT_OPEN = AsciiString.cached("open");

    /**
     * GET /api/test/sse-events
     * <p>
     * A demo API for the low level {@code SSE} implementation.
     *
     * @param ctx request context
     * @param len the length of the event message
     * @return result
     * @since 3.9
     */
    @HttpGet("/test/sse-events")
    public CompletionStage<HttpResult> getTestSseEvents(HttpRequestContext ctx, @QueryVar(value = "len", required = false) Integer len) {
        // GET /api/test/sse-events
        System.out.println("-- test sse-events --");
        System.out.println(ctx.channel());
        int messageSize = len == null ? 100 : len;
        var channel = ctx.channel();
        var pipeline = channel.pipeline();
        var timeoutHandler = pipeline.get(TIMEOUT_HANDLER);
        boolean hasReadTimeout;
        if (timeoutHandler instanceof ReadTimeoutHandler) {
            pipeline.remove(TIMEOUT_HANDLER);
            hasReadTimeout = true;
        } else {
            hasReadTimeout = false;
        }
        var response = ctx.responseFactory().create(OK);
        HttpUtil.setTransferEncodingChunked(response, true);
        response.headers().set(CONTENT_TYPE, TEXT_EVENT_STREAM);
        response.headers().set(CONTENT_ENCODING, IDENTITY);
        var future = new CompletableFuture<HttpResult>();
        channel.writeAndFlush(response).addListener((ChannelFuture cf) -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, -1, OK));

            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        });
        channel.writeAndFlush(SseEventBuilder.pingEvent().serialize(ctx.alloc()));
        // id: 1\n\n
        channel.writeAndFlush(SseEventBuilder.create().id(0).build().serialize(ctx.alloc()));
        var writeStreamTask = new Runnable() {

            private int n;

            @Override
            public void run() {
                if (n++ < messageSize) {
                    var data = new AsciiString(Fastjson2Library.getInstance().dumpsToBytes(Map.of("line", n)));
                    channel.writeAndFlush(SseEventBuilder.message(data).id(n).build().serialize(channel.alloc()));
                    channel.eventLoop().schedule(this, 1000, TimeUnit.MILLISECONDS);
                } else {
                    channel.writeAndFlush(SseEventBuilder.create().event(SSE_EVENT_CLOSE).build().serialize(channel.alloc()));
                    channel.writeAndFlush(EMPTY_LAST_CONTENT).addListener(READ_NEXT);
                    if (hasReadTimeout) {
                        var readTimeoutHandler = (ReadTimeoutHandler) timeoutHandler;
                        channel.pipeline().addFirst(TIMEOUT_HANDLER, new ReadTimeoutHandler(readTimeoutHandler.getReaderIdleTimeInMillis(), TimeUnit.MILLISECONDS));
                    }
                }
            }
        };
        channel.eventLoop().schedule(writeStreamTask, 1000, TimeUnit.MILLISECONDS);
        return future;
    }

    /**
     * GET /api/test/sse-event-stream
     * <p>
     * A demo API for the high level {@code SSE} implementation.
     *
     * @param ctx request context
     * @param len the length of the event message
     * @return result
     * @since 3.9
     */
    @SuppressWarnings("CallToPrintStackTrace")
    @HttpGet("/test/sse-event-stream")
    public CompletionStage<HttpResult> getTestSseEventStream(HttpRequestContext ctx, @QueryVar(value = "len", required = false) Integer len) {
        // GET /api/test/sse-event-stream
        System.out.println("-- test sse-event-stream --");
        System.out.println(ctx.channel());
        int messageSize = len == null ? 100 : len;
        var eventLoop = ctx.eventLoop();
        var running = new AtomicBoolean(false);
        var uuid = UUID.randomUUID().toString();
        return ctx.eventStreamBuilder().autoPing().onError((stream, cause) -> {
            System.err.println("error occurs on SSE event stream");
            cause.printStackTrace();
            running.set(false);
        }).onActive(stream -> {
            running.set(true);
            // id: 1\n
            // event: open\n\n
            // data: {"session":"$uuid"}
            var sessionData = AsciiString.of(Fastjson2Library.getInstance().dumpsToString(Map.of("session", uuid)));
            stream.sendEvent(SseEventBuilder.create().id(1).event(SSE_EVENT_OPEN).data(sessionData));
            var writeStreamTask = new Runnable() {

                private int n;

                @Override
                public void run() {
                    if (!running.get()) {
                        System.err.println("Abnormal interruption of event stream");
                        return;
                    }
                    if (n++ < messageSize) {
                        var data = new AsciiString(Fastjson2Library.getInstance().dumpsToBytes(Map.of("line", n)));
                        stream.sendEvent(SseEventBuilder.message(data).id(n + 1));
                        eventLoop.schedule(this, 1000, TimeUnit.MILLISECONDS);
                    } else {
                        stream.sendEvent(SseEventBuilder.create().event(SSE_EVENT_CLOSE).id(n + 1).data(sessionData));
                        stream.close();
                    }
                }
            };
            eventLoop.schedule(writeStreamTask, 1000, TimeUnit.MILLISECONDS);
        }).build().start();
    }

}
