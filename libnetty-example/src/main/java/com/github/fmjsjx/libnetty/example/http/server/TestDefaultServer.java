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
import java.util.NoSuchElementException;
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
import com.github.fmjsjx.libnetty.http.server.annotation.GetRoute;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.PostRoute;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.LogFormat;
import com.github.fmjsjx.libnetty.http.server.middleware.AccessLogger.Slf4jLoggerWrapper;
import com.github.fmjsjx.libnetty.http.server.middleware.Router;
import com.github.fmjsjx.libnetty.http.server.middleware.ServeStatic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
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
                .addLast(new ServeStatic("/static/", "src/main/resources/static/"))
                .addLast(new Router().get("/test", controller::getTest).get("/errors/{code}", ctx -> {
                    int code;
                    try {
                        code = ctx.pathVariables().getInt("code").getAsInt();
                    } catch (NumberFormatException | NoSuchElementException e) {
                        return HttpServerUtil.respond(ctx, BAD_REQUEST);
                    }
                    return controller.getErrors(ctx, code);
                }).get("/jsons", controller::getJsons).post("/echo", controller::postEcho).init());
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

class TestController {

    static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT);

    @GetRoute("/test")
    CompletionStage<HttpResult> getTest(HttpRequestContext ctx) {
        // GET /test
        System.out.println("-- test --");
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
        System.out.println(body.toString(CharsetUtil.UTF_8));
        return HttpServerUtil.respond(ctx, OK, body, TEXT_PLAIN);
    }

    @GetRoute("/errors/{code}")
    CompletionStage<HttpResult> getErrors(HttpRequestContext ctx, @PathVar int code) {
        // GET /errors/{code}
        System.out.println("-- errors --");
        HttpResponseStatus status = HttpResponseStatus.valueOf(code);
        System.out.println("status: ==> " + status);
        return HttpServerUtil.respond(ctx, status);
    }

    @GetRoute("/jsons")
    CompletionStage<HttpResult> getJsons(HttpRequestContext ctx) {
        // GET /jsons
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
        return HttpServerUtil.respondJson(ctx, OK, body);
    }

    @PostRoute("/echo")
    CompletionStage<HttpResult> postEcho(HttpRequestContext ctx) {
        // POST /echo
        System.out.println("-- echo --");
        ByteBuf content = ctx.request().content();
        Charset charset = HttpUtil.getCharset(ctx.request(), CharsetUtil.UTF_8);
        System.out.println(content.toString(charset));
        CharSequence contentType = ctx.contentType().orElseGet(() -> contentType(TEXT_PLAIN, charset));
        return HttpServerUtil.respond(ctx, OK, content.retain(), contentType);
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
