package com.github.fmjsjx.libnetty.transport;

import io.netty.channel.IoHandlerFactory;
import io.netty.channel.uring.IoUringIoHandler;


/**
 * The abstract implementation of {@link TransportLibrary} that uses
 * <a href="https://en.wikipedia.org/wiki/Io_uring">io_uring</a>.
 *
 * @since 3.8
 */
abstract class AbstractIoUringTransportLibrary implements TransportLibrary {

    /**
     * Returns a new {@link IoHandlerFactory} that creates
     * {@link IoUringIoHandler} instances.
     */
    @Override
    public IoHandlerFactory createIoHandlerFactory() {
        return IoUringIoHandler.newFactory();
    }

}
