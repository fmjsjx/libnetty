package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses EPOLL.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public class EpollTransportLibrary extends AbstractEpollTransportLibrary {

    private static final class InstanceHolder {
        private static final EpollTransportLibrary instance = new EpollTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton {@link EpollTransportLibrary} instance
     */
    public static final EpollTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Class<EpollSocketChannel> channelClass() {
        return EpollSocketChannel.class;
    }

    @Override
    public Class<EpollServerSocketChannel> serverChannelClass() {
        return EpollServerSocketChannel.class;
    }

}
