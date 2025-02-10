package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;

/**
 * {@link IoTransportLibrary} implementations which uses
 * <a href="https://en.wikipedia.org/wiki/Io_uring">io_uring</a>.
 *
 * @author MJ Fang
 * @since 3.8
 */
public class IoUringIoTransportLibrary extends AbstractIoTransportLibrary {

    private static final class InstanceHolder {
        private static final IoUringIoTransportLibrary INSTANCE = new IoUringIoTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link IoUringIoTransportLibrary} instance
     */
    public static final IoUringIoTransportLibrary getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Constructs a new {@link IoUringIoTransportLibrary} instance.
     */
    public IoUringIoTransportLibrary() {
        super(IoUringIoHandler::newFactory);
    }

    @Override
    public Class<IoUringSocketChannel> channelClass() {
        return IoUringSocketChannel.class;
    }

    @Override
    public Class<IoUringDatagramChannel> datagramChannelClass() {
        return IoUringDatagramChannel.class;
    }

    @Override
    public Class<IoUringServerSocketChannel> serverChannelClass() {
        return IoUringServerSocketChannel.class;
    }
}
