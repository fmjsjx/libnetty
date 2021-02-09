package com.github.fmjsjx.libnetty.example.http.server;

import static com.github.fmjsjx.libnetty.http.HttpCommonUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.annotation.HeaderValue;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpGet;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPost;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.RemoteAddr;
import com.github.fmjsjx.libnetty.http.server.annotation.StringBody;
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

@HttpPath("/api")
public class TestController {

    static final AsciiString ASCII_OK = AsciiString.cached("OK");

    @HttpGet("/test")
    public CompletionStage<HttpResult> getTest(HttpRequestContext ctx) {
        // GET /api/test
        System.out.println("-- test --");
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
        System.out.println(body.toString(CharsetUtil.UTF_8));
        return ctx.simpleRespond(OK, body, TEXT_PLAIN);
    }

    @HttpGet("/errors/{code}")
    public CompletionStage<HttpResult> getErrors(HttpRequestContext ctx, @PathVar("code") int code,
            @RemoteAddr String clientIp, @HeaderValue("user-agent") Optional<String> userAgent) {
        // GET /api/errors/{code}
        System.out.println("-- errors --");
        System.out.println("client IP ==> " + clientIp);
        System.out.println("user agent ==> " + userAgent);
        HttpResponseStatus status = HttpResponseStatus.valueOf(code);
        System.out.println("status ==> " + status);
        return ctx.simpleRespond(status);
    }

    @HttpGet("/jsons")
    @JsonBody
    public CompletableFuture<?> getJsons(QueryStringDecoder query, EventLoop eventLoop) {
        // GET /api/jsons
        System.out.println("-- jsons --");
//        System.out.println("library: " + library);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        query.parameters().forEach((key, values) -> {
            if (values.size() == 1) {
                node.put(key, values.get(0));
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

    @HttpGet("/no-content")
    public CompletionStage<Void> getNoContent(QueryStringDecoder query, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            // GET /api/no-content
            System.out.println("-- no content --");
            System.out.println(query.uri());
        }, executor);
    }

    @HttpGet("/ok")
    @StringBody
    public CompletionStage<CharSequence> getOK(QueryStringDecoder query) {
        System.out.println("-- ok --");
        System.out.println(query.uri());
        return CompletableFuture.completedFuture(ASCII_OK);
    }

}
