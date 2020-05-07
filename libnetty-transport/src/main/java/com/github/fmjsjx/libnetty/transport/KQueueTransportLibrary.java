package com.github.fmjsjx.libnetty.transport;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses KQUEUE.
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
public class KQueueTransportLibrary implements TransportLibrary {

    private static final class InstanceHolder {
        private static final KQueueTransportLibrary instance = new KQueueTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton {@link KQueueTransportLibrary} instance
     */
    public static final KQueueTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Class<KQueueSocketChannel> channelClass() {
        return KQueueSocketChannel.class;
    }

    @Override
    public Class<KQueueServerSocketChannel> serverChannelClass() {
        return KQueueServerSocketChannel.class;
    }

    @Override
    public KQueueEventLoopGroup createGroup() {
        return new KQueueEventLoopGroup();
    }

    @Override
    public KQueueEventLoopGroup createGroup(int nThreads) {
        return new KQueueEventLoopGroup(nThreads);
    }

    @Override
    public KQueueEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new KQueueEventLoopGroup(nThreads, threadFactory);
    }

}
