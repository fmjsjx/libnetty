package com.github.fmjsjx.libnetty.handler.ssl;

import java.util.function.Function;

import io.netty.handler.ssl.SslContext;
import io.netty.util.Mapping;

/**
 * Provides setter functions for {@link SniHandlerProvider}.
 * 
 * @author MJ Fang
 * 
 * @see SniHandlerProvider
 * @see SniHandlerProviders
 *
 * @since 2.3
 */
public interface PermutableSniHandlerProvider extends SniHandlerProvider,
        Function<Mapping<? super String, ? extends SslContext>, Mapping<? super String, ? extends SslContext>> {

    @Override
    default Mapping<? super String, ? extends SslContext> apply(Mapping<? super String, ? extends SslContext> t) {
        return setMapping(t);
    }

    /**
     * Returns the current {@link Mapping}.
     * 
     * @return the {@link Mapping}
     */
    Mapping<? super String, ? extends SslContext> mapping();

    /**
     * Set the {@link Mapping}.
     * 
     * @param mapping the mapping of domain name to {@link SslContext}
     * @return the old {@link Mapping}
     */
    Mapping<? super String, ? extends SslContext> setMapping(Mapping<? super String, ? extends SslContext> mapping);

}
