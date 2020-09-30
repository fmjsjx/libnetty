package com.github.fmjsjx.libnetty.example.http.server;

import static com.github.fmjsjx.libnetty.http.HttpUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServerHandlerProvider;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerUtil;
import com.github.fmjsjx.libnetty.http.server.annotation.GetRoute;
import com.github.fmjsjx.libnetty.http.server.annotation.HeaderValue;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.PostRoute;
import com.github.fmjsjx.libnetty.http.server.annotation.RemoteAddr;
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.LogFormat;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.Slf4jLoggerWrapper;
import com.github.fmjsjx.libnetty.http.server.middleware.AuthBasic;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.middleware.ServeStatic;
import com.github.fmjsjx.libnetty.http.server.middleware.SupportJson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestDefaultServer {

    public static void main(String[] args) throws Exception {
        TestController controller = new TestController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServerHandlerProvider handlerProvider = new DefaultHttpServerHandlerProvider();
        handlerProvider.exceptionHandler((ctx, e) -> log.error("EEEEEEEEEEEEEEEEEEEEEEEEEEEE ==> {}", ctx.channel(), e))
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2))
                .addLast(new SupportJson())
                .addLast("/static/auth", new AuthBasic(Collections.singletonMap("test", "123456"), "test"))
                .addLast(new ServeStatic("/static/", "src/main/resources/static/"))
                .addLast(new Router().register(controller).init());
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

@HttpPath("/api")
class TestController {

    static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT);

    @GetRoute("/test")
    public CompletionStage<HttpResult> getTest(HttpRequestContext ctx) {
        // GET /test
        System.out.println("-- test --");
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
        System.out.println(body.toString(CharsetUtil.UTF_8));
        return HttpServerUtil.respond(ctx, OK, body, TEXT_PLAIN);
    }

    @GetRoute("/errors/{code}")
    public CompletionStage<HttpResult> getErrors(HttpRequestContext ctx, @PathVar("code") int code,
            @RemoteAddr String clientIp, @HeaderValue("user-agent") Optional<String> userAgent) {
        // GET /errors/{code}
        System.out.println("-- errors --");
        System.out.println("client IP ==> " + clientIp);
        System.out.println("user agent ==> " + userAgent);
        HttpResponseStatus status = HttpResponseStatus.valueOf(code);
        System.out.println("status ==> " + status);
        return HttpServerUtil.respond(ctx, status);
    }

    @GetRoute("/jsons")
    @JsonBody
    public CompletableFuture<?> getJsons(QueryStringDecoder query) {
        // GET /jsons
        System.out.println("-- jsons --");
        ObjectNode node = mapper.createObjectNode();
        query.parameters().forEach((key, values) -> {
            if (values.size() == 1) {
                for (String value : values) {
                    node.put(key, value);
                }
            } else {
                node.putPOJO(key, values);
            }
        });
        CompletableFuture<Object> future = new CompletableFuture<>();
        if (node.isEmpty()) {
            future.completeExceptionally(
                    new ManualHttpFailureException(BAD_REQUEST, "{\"code\":1,\"message\":\"Missing Query String\"}",
                            HttpHeaderValues.APPLICATION_JSON, "Missing Query String"));
        } else {
            future.complete(node);
        }
        return future;
    }

    @PostRoute("/echo")
    public CompletionStage<HttpResult> postEcho(HttpRequestContext ctx, @JsonBody JsonNode value) {
        // POST /echo
        System.out.println("-- echo --");
        System.out.println("value ==> " + value);
        ByteBuf content = ctx.request().content();
        Charset charset = HttpUtil.getCharset(ctx.request(), CharsetUtil.UTF_8);
        CharSequence contentType = ctx.contentType().orElseGet(() -> contentType(TEXT_PLAIN, charset));
        return HttpServerUtil.respond(ctx, OK, content.retain(), contentType);
    }

}
