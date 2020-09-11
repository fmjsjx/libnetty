package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

/**
 * A {@link Middleware} logging HTTP access logs.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class AccessLogger implements Middleware {

    @FunctionalInterface
    public interface LoggerWrapper {

        void log(String content);

        default boolean isEnabled() {
            return true;
        }

    }

    private static class StdoutLoggerWrapper implements LoggerWrapper {

        private static final StdoutLoggerWrapper INSTANCE = new StdoutLoggerWrapper();

        @Override
        public void log(String content) {
            System.out.println(content);
        }

    }

    private static Function<HttpResult, String> generateMapperFromPattern(String pattern) {
        // TODO Auto-generated method stub
        return null;
    }

    private final LoggerWrapper loggerWrapper;
    private final Function<HttpResult, String> logMapper;

    // TODO other public constructors

    public AccessLogger(String pattern) {
        this(StdoutLoggerWrapper.INSTANCE, generateMapperFromPattern(pattern));
    }

    private AccessLogger(LoggerWrapper loggerWrapper, Function<HttpResult, String> logMapper) {
        this.loggerWrapper = loggerWrapper;
        this.logMapper = logMapper;
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        return next.doNext(ctx).whenComplete((r, e) -> {
            if (e != null && loggerWrapper.isEnabled()) {
                loggerWrapper.log(logMapper.apply(r));
            }
        });
    }

}
