package com.github.fmjsjx.libnetty.http.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.http.exception.MultiErrorsException;
import com.github.fmjsjx.libnetty.http.server.middleware.Middleware;
import com.github.fmjsjx.libnetty.http.server.middleware.MiddlewareChain;
import com.github.fmjsjx.libnetty.http.server.middleware.MiddlewareChains;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

@Sharable
class DefaultHttpServerHandler extends HttpRequestContextHandler {

    private final List<Middleware> middlewares;
    private final MiddlewareChain firstChain;
    private final BiConsumer<ChannelHandlerContext, Throwable> exceptionHandler;

    DefaultHttpServerHandler(List<Middleware> middlewares, MiddlewareChain lastChain,
            BiConsumer<ChannelHandlerContext, Throwable> exceptionHandler) {
        this.middlewares = Collections.unmodifiableList(middlewares);
        ArrayList<Middleware> ml = new ArrayList<>(middlewares);
        Collections.reverse(ml);
        MiddlewareChain c = lastChain;
        for (Middleware m : ml) {
            c = MiddlewareChains.next(m, c);
        }
        this.firstChain = c;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        exceptionHandler.accept(ctx, cause);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpRequestContext msg) throws Exception {
        firstChain.doNext(msg);
    }

    void onServerClosed() throws MultiErrorsException, HttpRuntimeException {
        List<Throwable> errors = new ArrayList<>();
        for (Middleware middleware : middlewares) {
            try {
                middleware.onServerClosed();
            } catch (Exception e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            int size = errors.size();
            if (size == 1) {
                throw new HttpRuntimeException("Error occurs on server closed", errors.get(0));
            } else {
                throw new MultiErrorsException("Errors occurs on server closed", errors.toArray(new Throwable[size]));
            }
        }
    }

}
