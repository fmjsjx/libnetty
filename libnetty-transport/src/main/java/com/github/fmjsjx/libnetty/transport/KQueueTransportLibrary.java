package com.github.fmjsjx.libnetty.transport;

import com.github.fmjsjx.libnetty.transport.io.KQueueIoTransportLibrary;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;

import java.util.concurrent.ThreadFactory;

/**
 * {@link TransportLibrary} implementations which uses KQUEUE.
 *
 * @author MJ Fang
 * @since 1.0
 * @deprecated since 3.8, please use {@link KQueueIoTransportLibrary} instead
 */
@Deprecated
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

    /**
     * Constructs a new {@link KQueueTransportLibrary} instance.
     */
    public KQueueTransportLibrary() {
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
