package com.github.fmjsjx.libnetty.example.http.server;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.OptionalInt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpGet;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPost;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.QueryVar;
import com.github.fmjsjx.libnetty.http.server.annotation.StringBody;
import com.github.fmjsjx.libnetty.http.server.exception.ManualHttpFailureException;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * Blocking test controller
 */
@HttpPath("/api")
public class BlockingTestController {

    /**
     * Constructs a new {@link BlockingTestController} instance.
     */
    public BlockingTestController() {
    }

    /**
     * GET /api/jsons
     *
     * @param query query
     * @return result
     */
    @HttpGet("/jsons")
    @JsonBody
    public Object getJsons(QueryStringDecoder query) {
        // GET /jsons
        System.out.println("-- jsons --");
        System.out.println(Thread.currentThread());
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        query.parameters().forEach((key, values) -> {
            if (values.size() == 1) {
                node.put(key, values.getFirst());
            } else {
                node.putPOJO(key, values);
            }
        });
        if (node.isEmpty()) {
            throw new ManualHttpFailureException(BAD_REQUEST, "{\"code\":1,\"message\":\"Missing Query String\"}",
                    HttpHeaderValues.APPLICATION_JSON, "Missing Query String");
        } else {
            return node;
        }
    }

    /**
     * POST /api/echo
     *
     * @param ctx   the request context
     * @param value the JSON body value
     * @return the result
     */
    @HttpPost("/echo")
    @JsonBody
    public Object postEcho(HttpRequestContext ctx, @JsonBody JsonNode value) {
        // POST /echo
        System.out.println("-- echo --");
        System.out.println(Thread.currentThread());
        System.out.println("value ==> " + value);
        return value;
    }

    /**
     * GET /api/no-content
     *
     * @param query query
     */
    @HttpGet("/no-content")
    public void getNoContent(QueryStringDecoder query) {
        // GET /no-content
        System.out.println("-- no content --");
        System.out.println(query.uri());
    }

    /**
     * GET /api/ok
     *
     * @param query query
     * @return result
     */
    @HttpGet("/ok")
    @StringBody
    public CharSequence getOK(QueryStringDecoder query) {
        System.out.println("-- ok --");
        System.out.println(query.uri());
        return TestController.ASCII_OK;
    }

    /**
     * GET /api/error
     *
     * @param test query parameter test
     * @throws Exception any error occurs
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @HttpGet("/error")
    @JsonBody
    public void getError(@QueryVar("test") OptionalInt test) throws Exception {
        System.err.println("-- error --");
        System.err.println(test);
        if (test.orElse(0) == 1)
            throw new TestException("test error");
        throw new Exception("no test");
    }

}
