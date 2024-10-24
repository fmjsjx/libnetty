package com.github.fmjsjx.libnetty.transport;


import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses EPOLL and domain socket
 * channels.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class EpollDomainTransportLibrary extends AbstractEpollTransportLibrary {

    private static final class InstanceHolder {
        private static final EpollDomainTransportLibrary instance = new EpollDomainTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton {@link EpollDomainTransportLibrary} instance
     */
    public static final EpollDomainTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Class<EpollDomainSocketChannel> channelClass() {
        return EpollDomainSocketChannel.class;
    }

    @Override
    public Class<EpollServerDomainSocketChannel> serverChannelClass() {
        return EpollServerDomainSocketChannel.class;
    }

}
