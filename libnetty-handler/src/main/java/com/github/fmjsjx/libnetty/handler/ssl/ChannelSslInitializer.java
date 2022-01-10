package com.github.fmjsjx.libnetty.handler.ssl;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslHandler;

/**
 * Interface to support SSL feature when initialize a {@link Channel}.
 * 
 * @author MJ Fang
 *
 * @since 2.3
 */
@FunctionalInterface
public interface ChannelSslInitializer<C extends Channel> {

    /**
     * Returns the {@link ChannelSslInitializer} instance with the specified
     * {@link SslContextProvider} given.
     * 
     * @param <C>                the type of the {@link Channel}
     * @param sslContextProvider the {@link SslContextProvider}
     * @return the {@link ChannelSslInitializer} instance with the specified
     *         {@link SslContextProvider} given
     */
    static <C extends Channel> ChannelSslInitializer<C> of(SslContextProvider sslContextProvider) {
        return ch -> {
            ch.pipeline().addLast(SslHandler.class.getName(), sslContextProvider.get().newHandler(ch.alloc()));
            return ch;
        };
    }

    /**
     * Returns the {@link ChannelSslInitializer} instance with the specified
     * {@link SniHandlerProvider} given.
     * 
     * @param <C>                the type of the {@link Channel}
     * @param sniHandlerProvider the {@link SniHandlerProvider}
     * @return the {@link ChannelSslInitializer} instance with the specified
     *         {@link SniHandlerProvider} given
     */
    static <C extends Channel> ChannelSslInitializer<C> of(SniHandlerProvider sniHandlerProvider) {
        return ch -> {
            ch.pipeline().addLast(SniHandler.class.getName(), sniHandlerProvider.get());
            return ch;
        };
    }

    /**
     * Initialize the channel to support SSL feature.
     * 
     * @param channel the {@link Channel}
     * @return the input {@link Channel}
     * @throws Exception is thrown if an error occurs
     */
    C init(C channel) throws Exception;

}
