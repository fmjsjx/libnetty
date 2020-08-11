package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

public interface Middleware extends BiFunction<HttpRequestContext, MiddlewareChain, CompletionStage<HttpResult>> {

    @Override
    CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next);

}
