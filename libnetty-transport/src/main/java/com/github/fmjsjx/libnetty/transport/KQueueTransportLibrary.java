package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses KQUEUE.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public class KQueueTransportLibrary extends AbstractKQueueTransportLibrary {

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

}
