package com.github.fmjsjx.libnetty.transport;

import com.github.fmjsjx.libnetty.transport.io.EpollIoTransportLibrary;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;

import java.util.concurrent.ThreadFactory;

/**
 * {@link TransportLibrary} implementations which uses EPOLL.
 *
 * @author MJ Fang
 * @since 1.0
 * @deprecated since 3.8, please use {@link EpollIoTransportLibrary} instead
 */
@Deprecated
public class EpollTransportLibrary implements TransportLibrary {

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

    /**
     * Constructs a new {@link EpollTransportLibrary} instance.
     */
    public EpollTransportLibrary() {
    }

    @Override
    public Class<EpollSocketChannel> channelClass() {
        return EpollSocketChannel.class;
    }

    @Override
    public Class<EpollServerSocketChannel> serverChannelClass() {
        return EpollServerSocketChannel.class;
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
