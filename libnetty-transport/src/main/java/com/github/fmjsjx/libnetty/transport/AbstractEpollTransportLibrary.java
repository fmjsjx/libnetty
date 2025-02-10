package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.IoHandlerFactory;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;

import java.util.concurrent.ThreadFactory;

/**
 * The abstract implementation of {@link TransportLibrary} that uses EPOLL.
 *
 * @since 3.8
 */
@SuppressWarnings("deprecation")
abstract class AbstractEpollTransportLibrary implements TransportLibrary {

    @Override
    @Deprecated
    public EpollEventLoopGroup createGroup() {
        return new EpollEventLoopGroup();
    }

    @Override
    @Deprecated
    public EpollEventLoopGroup createGroup(int nThreads) {
        return new EpollEventLoopGroup(nThreads);
    }

    @Override
    @Deprecated
    public EpollEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new EpollEventLoopGroup(nThreads, threadFactory);
    }

    /**
     * Returns a new {@link IoHandlerFactory} that creates
     * {@link EpollIoHandler} instances.
     */
    @Override
    public IoHandlerFactory createIoHandlerFactory() {
        return EpollIoHandler.newFactory();
    }

}
