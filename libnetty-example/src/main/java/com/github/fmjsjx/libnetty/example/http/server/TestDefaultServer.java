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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorFactory;
import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpGet;
import com.github.fmjsjx.libnetty.http.server.annotation.HeaderValue;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPost;
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
import io.netty.channel.EventLoop;
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

    private static final Map<String, String> passwds() {
        return Collections.singletonMap("test", "123456");
    }

    public static void main(String[] args) throws Exception {
        TestController controller = new TestController();
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowedRequestMethods(GET, POST, PUT, PATCH, DELETE)
                .allowedRequestHeaders("*").allowNullOrigin().build();
        DefaultHttpServer server = new DefaultHttpServer("test", 8443) // server name and port
                .enableSsl(SslContextProviders.selfSignedForServer()) // SSL
                .neverTimeout() // never timeout
                .corsConfig(corsConfig) // CORS support
                .ioThreads(1) // IO threads (event loop)
                .maxContentLength(10 * 1024 * 1024) // MAX content length -> 10 MB
                .soBackLog(1024).tcpNoDelay() // channel options
                .applyCompressionSettings( // compression support
                        HttpContentCompressorFactory.defaultSettings()) // default settings
//                        b -> b.compressionLevel(1).memLevel(1).windowBits(9).contentSizeThreshold(4096)) // fastest
//                        b -> b.compressionLevel(9).memLevel(9).windowBits(15).contentSizeThreshold(512)) // best
        ;
        server.defaultHandlerProvider() // use default server handler (DefaultHttpServerHandlerProvider)
                .addLast(new AccessLogger(new Slf4jLoggerWrapper("accessLogger"), LogFormat.BASIC2)) // access logger
                .addLast(new SupportJson()) // JSON support
                .addLast("/static/auth", new AuthBasic(passwds(), "test")) // HTTP Basic Authentication
                .addLast(new ServeStatic("/static/", "src/main/resources/static/")) // static resources
                .addLast(new Router().register(controller).init()) // router
        ;
        try {
            server.startup();
            log.info("Server {} started.", server);
            System.in.read();
        } catch (Exception e) {
            log.error("Unexpected error occurs when startup {}", server, e);
        } finally {
            if (server.isRunning()) {
                server.shutdown();
                log.info("Server {} stopped.", server);
            }
        }
    }

}

@HttpPath("/api")
class TestController {

    static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT);

    @HttpGet("/test")
    public CompletionStage<HttpResult> getTest(HttpRequestContext ctx) {
        // GET /test
        System.out.println("-- test --");
        // always returns 200 OK
        ByteBuf body = ByteBufUtil.writeAscii(ctx.alloc(), "200 OK");
        System.out.println(body.toString(CharsetUtil.UTF_8));
        return ctx.simpleRespond(OK, body, TEXT_PLAIN);
    }

    @HttpGet("/errors/{code}")
    public CompletionStage<HttpResult> getErrors(HttpRequestContext ctx, @PathVar("code") int code,
            @RemoteAddr String clientIp, @HeaderValue("user-agent") Optional<String> userAgent) {
        // GET /errors/{code}
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
        // GET /jsons
        System.out.println("-- jsons --");
        ObjectNode node = mapper.createObjectNode();
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
        // POST /echo
        System.out.println("-- echo --");
        System.out.println("value ==> " + value);
        ByteBuf content = ctx.request().content();
        Charset charset = HttpUtil.getCharset(ctx.request(), CharsetUtil.UTF_8);
        CharSequence contentType = ctx.contentType().orElseGet(() -> contentType(TEXT_PLAIN, charset));
        return ctx.simpleRespond(OK, content.retain(), contentType);
    }

}
