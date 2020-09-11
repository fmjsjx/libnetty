package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServerHandlerProvider;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerUtil;
import com.github.fmjsjx.libnetty.http.server.middleware.Middleware;
import com.github.fmjsjx.libnetty.http.server.middleware.MiddlewareChain;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestDefaultServer {

    public static void main(String[] args) throws Exception {
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServerHandlerProvider handlerProvider = new DefaultHttpServerHandlerProvider();
        handlerProvider.exceptionHandler((ctx, e) -> log.error("EEEEEEEEEEEEEEEEEEEEEEEEEEEE ==> {}", ctx.channel(), e))
                .addLast(new Router());
        DefaultHttpServer server = new DefaultHttpServer("test", SslContextProviders.selfSignedForServer(), 8443)
                .corsConfig(corsConfig).ioThreads(1).maxContentLength(10 * 1024 * 1024).soBackLog(1024).tcpNoDelay()
                .handlerProvider(handlerProvider);
        try {
            server.startup();
            System.in.read();
        } catch (Exception e) {
            System.err.println("Unexpected error occurs when startup " + server);
            e.printStackTrace();
        } finally {
            if (server.isRunning()) {
                server.shutdown();
            }
        }
    }

}

class Router implements Middleware {

    static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT);

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        System.out.println(ctx);
        System.out.println();
        if (ctx.rawPath().startsWith("/test")) {
            System.out.println("-- test --");
            // always returns 200 OK
            ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
            System.out.println(body.toString(CharsetUtil.UTF_8));
            return HttpServerUtil.respond(ctx, HttpResponseStatus.OK, body, HttpHeaderValues.TEXT_PLAIN);
        } else if (ctx.queryStringDecoder().path().matches("/errors/\\d+/?")) {
            System.out.println("-- errors --");
            String[] paths = ctx.queryStringDecoder().path().split("/");
            int code = Integer.parseInt(paths[2]);
            HttpResponseStatus status = HttpResponseStatus.valueOf(code);
            System.out.println("status: ==> " + status);
            return HttpServerUtil.respond(ctx, status);
        } else if (ctx.queryStringDecoder().path().matches("/jsons/?")) {
            System.out.println("-- jsons --");
            ObjectNode node = mapper.createObjectNode();
            ctx.queryStringDecoder().parameters().forEach((key, values) -> {
                if (values.size() == 1) {
                    for (String value : values) {
                        node.put(key, value);
                    }
                } else {
                    node.putPOJO(key, values);
                }
            });
            String value = toJson(node);
            ByteBuf body = ByteBufUtil.writeUtf8(ctx.alloc(), value);
            System.out.println(body.toString(CharsetUtil.UTF_8));
            return HttpServerUtil.respondJson(ctx, HttpResponseStatus.OK, body);
        }
        return next.doNext(ctx);
    }

    private static final String toJson(ObjectNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            // ignore
        }
        return "null";
    }

}
