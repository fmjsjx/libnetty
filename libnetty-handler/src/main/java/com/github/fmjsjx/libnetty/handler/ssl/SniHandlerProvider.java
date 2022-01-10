package com.github.fmjsjx.libnetty.handler.ssl;

import java.util.function.Supplier;

import io.netty.handler.ssl.SniHandler;

/**
 * Provides {@link SniHandler} instances.
 * 
 * @author MJ Fang
 * 
 * @see PermutableSniHandlerProvider
 * @see SniHandlerProviders
 *
 * @since 2.3
 */
@FunctionalInterface
public interface SniHandlerProvider extends Supplier<SniHandler> {

    /**
     * Returns a new {@link SniHandler} instance.
     * 
     * @return a new {@code SniHandler}
     */
    @Override
    SniHandler get();

}
