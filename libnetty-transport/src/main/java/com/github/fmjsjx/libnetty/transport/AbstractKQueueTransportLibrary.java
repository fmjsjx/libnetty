package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.IoHandlerFactory;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueIoHandler;

import java.util.concurrent.ThreadFactory;

/**
 * The abstract implementation of {@link TransportLibrary} that uses KQUEUE.
 *
 * @since 3.8
 */
abstract class AbstractKQueueTransportLibrary implements TransportLibrary {

    @Override
    @Deprecated
    public KQueueEventLoopGroup createGroup() {
        return new KQueueEventLoopGroup();
    }

    @Override
    @Deprecated
    public KQueueEventLoopGroup createGroup(int nThreads) {
        return new KQueueEventLoopGroup(nThreads);
    }

    @Override
    @Deprecated
    public KQueueEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new KQueueEventLoopGroup(nThreads, threadFactory);
    }

    /**
     * Returns a new {@link IoHandlerFactory} that creates
     * {@link KQueueIoHandler} instances.
     */
    @Override
    public IoHandlerFactory createIoHandlerFactory() {
        return KQueueIoHandler.newFactory();
    }

}
