package com.github.fmjsjx.libnetty.transport;


import com.github.fmjsjx.libnetty.transport.io.EpollIoTransportLibrary;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;

import java.util.concurrent.ThreadFactory;

/**
 * {@link TransportLibrary} implementations which uses EPOLL and domain socket
 * channels.
 *
 * @author MJ Fang
 * @since 1.0
 * @deprecated since 3.8, please use {@link EpollIoTransportLibrary} instead
 */
@Deprecated
public class EpollDomainTransportLibrary implements TransportLibrary {

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

    @Override
    public EpollEventLoopGroup createGroup() {
        return new EpollEventLoopGroup();
    }

    @Override
    public EpollEventLoopGroup createGroup(int nThreads) {
        return new EpollEventLoopGroup(nThreads);
    }

    @Override
    public EpollEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new EpollEventLoopGroup(nThreads, threadFactory);
    }
}
