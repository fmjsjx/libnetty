package com.github.fmjsjx.libnetty.handler.ssl;

import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Mapping;

/**
 * The abstract implementation of {@link SniHandlerProvider}.
 * 
 * @author MJ Fang
 *
 * @since 2.3
 */
abstract class AbstractSniHandlerProvider implements SniHandlerProvider {

    protected abstract Mapping<? super String, ? extends SslContext> mapping();

    @Override
    public SniHandler get() {
        return new SniHandler(mapping());
    }

}
