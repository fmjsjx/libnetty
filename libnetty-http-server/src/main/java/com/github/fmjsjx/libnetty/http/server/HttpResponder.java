package com.github.fmjsjx.libnetty.http.server;

import java.util.function.Function;

@FunctionalInterface
public interface HttpResponder extends Function<HttpRequestContext, HttpResult> {
    
    @Override
    default HttpResult apply(HttpRequestContext t) {
        return respond(t);
    }

    HttpResult respond(HttpRequestContext ctx);

}
