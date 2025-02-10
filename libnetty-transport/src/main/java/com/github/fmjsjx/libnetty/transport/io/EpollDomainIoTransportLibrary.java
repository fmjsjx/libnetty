package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.epoll.*;

/**
 * {@link IoTransportLibrary} implementations which uses EPOLL and domain socket
 * channels.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class EpollDomainIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final EpollDomainIoTransportLibrary instance = new EpollDomainIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link EpollDomainIoTransportLibrary} instance
     */
    public static final EpollDomainIoTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Constructs a new {@link EpollDomainIoTransportLibrary} instance.
     */
    public EpollDomainIoTransportLibrary() {
        super(EpollIoHandler::newFactory);
    }

    @Override
    public Class<EpollDomainSocketChannel> channelClass() {
        return EpollDomainSocketChannel.class;
    }

    @Override
    public Class<EpollDomainDatagramChannel> datagramChannelClass() {
        return EpollDomainDatagramChannel.class;
    }

    @Override
    public Class<EpollServerDomainSocketChannel> serverChannelClass() {
        return EpollServerDomainSocketChannel.class;
    }

}
