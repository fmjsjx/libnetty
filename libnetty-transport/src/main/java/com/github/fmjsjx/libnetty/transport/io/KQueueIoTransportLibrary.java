package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;

/**
 * {@link IoTransportLibrary} implementations which uses KQUEUE.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class KQueueIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final KQueueIoTransportLibrary instance = new KQueueIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link KQueueIoTransportLibrary} instance
     */
    public static final KQueueIoTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Constructs a new {@link KQueueIoTransportLibrary} instance.
     */
    public KQueueIoTransportLibrary() {
        super(KQueueIoHandler::newFactory);
    }

    @Override
    public Class<KQueueSocketChannel> channelClass() {
        return KQueueSocketChannel.class;
    }

    @Override
    public Class<KQueueDatagramChannel> datagramChannelClass() {
        return KQueueDatagramChannel.class;
    }

    @Override
    public Class<KQueueServerSocketChannel> serverChannelClass() {
        return KQueueServerSocketChannel.class;
    }

}
