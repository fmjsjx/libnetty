package com.github.fmjsjx.libnetty.handler.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.util.Mapping;

/**
 * The default implementation of {@link SniHandlerProvider}.
 * 
 * @author MJ Fang
 * 
 * @since 2.3
 */
public class DefaultSniHandlerProvider extends AbstractSniHandlerProvider {

    private final Mapping<? super String, ? extends SslContext> mapping;

    /**
     * Creates a new {@link DefaultSniHandlerProvider} with the specified
     * {@code mapping} given.
     * 
     * @param mapping the mapping of domain name to SslContext
     */
    public DefaultSniHandlerProvider(Mapping<? super String, ? extends SslContext> mapping) {
        this.mapping = mapping;
    }

    @Override
    protected Mapping<? super String, ? extends SslContext> mapping() {
        return mapping;
    }

}