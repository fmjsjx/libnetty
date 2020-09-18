package com.github.fmjsjx.libnetty.http.server;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.handler.codec.http.HttpMethod;

/**
 * The abstract implementation of {@link ApiMatcher}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public abstract class AbstractApiMatcher implements ApiMatcher {

    protected final Pattern pathPattern;
    protected final Predicate<HttpMethod> methodMatcher;

    protected final ThreadLocal<Matcher> threadLocalMatcher = new ThreadLocal<Matcher>() {
        protected Matcher initialValue() {
            return pathPattern.matcher("");
        }
    };

    protected AbstractApiMatcher(Pattern pathPattern, Predicate<HttpMethod> methodMatcher) {
        this.pathPattern = pathPattern;
        this.methodMatcher = methodMatcher;
    }

    protected Matcher matcher() {
        return threadLocalMatcher.get();
    }

    protected Matcher matcher(String path) {
        return matcher().reset(path);
    }

    @Override
    public boolean pathMatches(HttpRequestContext ctx) {
        return matcher(ctx.path()).matches();
    }

    @Override
    public boolean methodMatched(HttpRequestContext ctx) {
        return methodMatcher.test(ctx.method());
    }

}
