package com.github.fmjsjx.libnetty.http.client;

import java.util.function.Supplier;

import io.netty.handler.proxy.ProxyHandler;

/**
 * A factory to create {@link ProxyHandler}s.
 * 
 * @since 1.2
 * 
 * @param <T> the type of {@link ProxyHandler} implementation
 *
 * @author MJ Fang
 * 
 */
public interface ProxyHandlerFactory<T extends ProxyHandler> extends Supplier<T> {

    /**
     * Creates a new {@link ProxyHandler} instance.
     * 
     * @return a new {@code ProxyHandler} instance
     */
    T create();

    @Override
    default T get() {
        return create();
    }

}
