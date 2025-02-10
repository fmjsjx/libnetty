package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.*;

/**
 * {@link IoTransportLibrary} implementations which uses NIO and domain
 * socket channels.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class NioDomainIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final NioDomainIoTransportLibrary instance = new NioDomainIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link NioDomainIoTransportLibrary} instance
     */
    public static final NioDomainIoTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Constructs a new {@link NioDomainIoTransportLibrary} instance.
     */
    public NioDomainIoTransportLibrary() {
        super(NioIoHandler::newFactory);
    }

    @Override
    public Class<NioDomainSocketChannel> channelClass() {
        return NioDomainSocketChannel.class;
    }

    @Override
    public Class<NioDatagramChannel> datagramChannelClass() {
        return NioDatagramChannel.class;
    }

    @Override
    public Class<NioServerDomainSocketChannel> serverChannelClass() {
        return NioServerDomainSocketChannel.class;
    }

}
