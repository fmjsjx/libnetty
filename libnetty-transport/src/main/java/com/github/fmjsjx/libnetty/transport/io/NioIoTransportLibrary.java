package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;


/**
 * {@link IoTransportLibrary} implementations which uses NIO.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class NioIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final NioIoTransportLibrary instance = new NioIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link NioIoTransportLibrary} instance
     */
    public static final NioIoTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Constructs a new {@link NioIoTransportLibrary} instance.
     */
    public NioIoTransportLibrary() {
        super(NioIoHandler::newFactory);
    }

    @Override
    public Class<NioSocketChannel> channelClass() {
        return NioSocketChannel.class;
    }

    @Override
    public Class<NioDatagramChannel> datagramChannelClass() {
        return NioDatagramChannel.class;
    }

    @Override
    public Class<NioServerSocketChannel> serverChannelClass() {
        return NioServerSocketChannel.class;
    }

}
