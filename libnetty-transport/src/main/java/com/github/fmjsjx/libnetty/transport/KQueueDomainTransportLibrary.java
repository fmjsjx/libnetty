package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses KQUEUE and domain socket
 * channels.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public class KQueueDomainTransportLibrary extends AbstractKQueueTransportLibrary {

    private static final class InstanceHolder {
        private static final KQueueDomainTransportLibrary instance = new KQueueDomainTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton {@link KQueueDomainTransportLibrary} instance
     */
    public static final KQueueDomainTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Class<KQueueDomainSocketChannel> channelClass() {
        return KQueueDomainSocketChannel.class;
    }

    @Override
    public Class<KQueueServerDomainSocketChannel> serverChannelClass() {
        return KQueueServerDomainSocketChannel.class;
    }

}
