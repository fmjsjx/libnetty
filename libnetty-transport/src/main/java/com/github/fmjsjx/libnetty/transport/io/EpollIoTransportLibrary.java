package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;

/**
 * {@link IoTransportLibrary} implementations which uses EPOLL.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class EpollIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final EpollIoTransportLibrary instance = new EpollIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link EpollIoTransportLibrary} instance
     */
    public static final EpollIoTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Constructs a new {@link EpollIoTransportLibrary} instance.
     */
    public EpollIoTransportLibrary() {
        super(EpollIoHandler::newFactory);
    }

    @Override
    public Class<EpollSocketChannel> channelClass() {
        return EpollSocketChannel.class;
    }

    @Override
    public Class<EpollDatagramChannel> datagramChannelClass() {
        return EpollDatagramChannel.class;
    }

    @Override
    public Class<EpollServerSocketChannel> serverChannelClass() {
        return EpollServerSocketChannel.class;
    }

}
