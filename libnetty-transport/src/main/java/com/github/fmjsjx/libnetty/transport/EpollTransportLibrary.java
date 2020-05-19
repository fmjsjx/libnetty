package com.github.fmjsjx.libnetty.transport;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses EPOLL.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
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
