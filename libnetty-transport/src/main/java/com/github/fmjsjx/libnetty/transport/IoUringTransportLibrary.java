package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;

/**
 * {@link TransportLibrary} implementations which uses
 * <a href="https://en.wikipedia.org/wiki/Io_uring">io_uring</a>.
 * 
 * @since 3.8
 * 
 * @author MJ Fang
 */
public class IoUringTransportLibrary extends AbstractIoUringTransportLibrary {

    private static final class InstanceHolder {
        private static final IoUringTransportLibrary instance = new IoUringTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton {@link IoUringTransportLibrary} instance
     */
    public static final IoUringTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Class<IoUringSocketChannel> channelClass() {
        return IoUringSocketChannel.class;
    }

    @Override
    public Class<IoUringServerSocketChannel> serverChannelClass() {
        return IoUringServerSocketChannel.class;
    }

}
