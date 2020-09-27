package com.github.fmjsjx.libnetty.http.server;

import static com.github.fmjsjx.libnetty.http.server.middleware.PathFilterMiddleware.toFilter;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.github.fmjsjx.libnetty.http.server.middleware.Middleware;
import com.github.fmjsjx.libnetty.http.server.middleware.MiddlewareChain;
import com.github.fmjsjx.libnetty.http.server.middleware.MiddlewareChains;
import com.github.fmjsjx.libnetty.http.server.middleware.PathFilterMiddleware;

import io.netty.channel.ChannelHandlerContext;

/**
 * The default implementation of {@link HttpServerHandlerProvider}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultHttpServerHandlerProvider implements HttpServerHandlerProvider {

    private static final MiddlewareChain DEFAULT_LAST_CHAIN = MiddlewareChains.notFound();
    // default just close the channel when any error occurs
    private static final BiConsumer<ChannelHandlerContext, Throwable> DEFAULT_EXCEPTION_HANDLER = (ctx, e) -> ctx
            .channel().close();

    private MiddlewareChain lastChain = DEFAULT_LAST_CHAIN;

    private LinkedList<Middleware> middlewares = new LinkedList<Middleware>();

    private BiConsumer<ChannelHandlerContext, Throwable> exceptionHandler = DEFAULT_EXCEPTION_HANDLER;

    private volatile DefaultHttpServerHandler value;

    @Override
    public DefaultHttpServerHandler get() {
        DefaultHttpServerHandler value = this.value;
        if (value == null) {
            synchronized (this) {
                if ((value = this.value) == null) {
                    this.value = value = initHandler();
                }
            }
        }
        return value;
    }

    private DefaultHttpServerHandler initHandler() {
        return new DefaultHttpServerHandler(middlewares, lastChain, exceptionHandler);
    }

    public DefaultHttpServerHandlerProvider lastChain(MiddlewareChain lastChain) {
        this.lastChain = Objects.requireNonNull(lastChain, "lastChain must not be null");
        return this;
    }

    public DefaultHttpServerHandlerProvider exceptionHandler(
            BiConsumer<ChannelHandlerContext, Throwable> exceptionHandler) {
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler must not be null")
                .andThen(DEFAULT_EXCEPTION_HANDLER);
        return this;
    }

    public DefaultHttpServerHandlerProvider add(Middleware middleware) {
        return addLast(middleware);
    }

    public DefaultHttpServerHandlerProvider add(Predicate<String> pathFilter, Middleware middleware) {
        return addLast(pathFilter, middleware);
    }

    public DefaultHttpServerHandlerProvider add(String path, Middleware middleware) {
        return addLast(path, middleware);
    }

    public DefaultHttpServerHandlerProvider addLast(Predicate<String> pathFilter, Middleware middleware) {
        return addLast(new PathFilterMiddleware(pathFilter, middleware));
    }

    public DefaultHttpServerHandlerProvider addLast(String path, Middleware middleware) {
        return add(toFilter(path), middleware);
    }

    public DefaultHttpServerHandlerProvider addLast(Middleware... middlewares) {
        LinkedList<Middleware> m = this.middlewares;
        for (Middleware middleware : middlewares) {
            m.addLast(Objects.requireNonNull(middleware, "middleware must not be null"));
        }
        return this;
    }

    public DefaultHttpServerHandlerProvider addFirst(Middleware middleware) {
        middlewares.addFirst(Objects.requireNonNull(middleware, "middleware must not be null"));
        return this;
    }

    public DefaultHttpServerHandlerProvider addFirst(Predicate<String> pathFilter, Middleware middleware) {
        return addFirst(new PathFilterMiddleware(pathFilter, middleware));
    }

    public DefaultHttpServerHandlerProvider addFirst(String path, Middleware middleware) {
        return addFirst(toFilter(path), middleware);
    }

}
