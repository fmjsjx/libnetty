package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.kqueue.*;

/**
 * {@link IoTransportLibrary} implementations which uses KQUEUE and
 * domain socket channels.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class KQueueDomainIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final KQueueDomainIoTransportLibrary instance = new KQueueDomainIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link KQueueDomainIoTransportLibrary} instance
     */
    public static final KQueueDomainIoTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Constructs a new {@link KQueueDomainIoTransportLibrary} instance.
     */
    public KQueueDomainIoTransportLibrary() {
        super(KQueueIoHandler::newFactory);
    }

    @Override
    public Class<KQueueDomainSocketChannel> channelClass() {
        return KQueueDomainSocketChannel.class;
    }

    @Override
    public Class<KQueueDomainDatagramChannel> datagramChannelClass() {
        return KQueueDomainDatagramChannel.class;
    }

    @Override
    public Class<KQueueServerDomainSocketChannel> serverChannelClass() {
        return KQueueServerDomainSocketChannel.class;
    }

}
