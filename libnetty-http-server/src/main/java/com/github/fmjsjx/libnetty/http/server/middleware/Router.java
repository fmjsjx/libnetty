package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

/**
 * A {@link Middleware} routing requests.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class Router implements Middleware {

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        // TODO Auto-generated method stub
        return null;
    }

}
