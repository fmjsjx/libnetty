package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;

import java.util.concurrent.ThreadFactory;

/**
 * Interface for netty I/O transport library.
 *
 * @author MJ Fang
 * @since 3.8
 */
public interface IoTransportLibrary {

    /**
     * Returns the default {@link IoTransportLibrary} instance, native
     * library is preferred.
     *
     * @return the default {@link IoTransportLibrary}
     */
    static IoTransportLibrary getDefault() {
        return IoTransportLibraries.getDefault();
    }

    /**
     * Returns the class of {@link Channel}.
     *
     * @return the {@link Class} of {@link Channel}
     */
    Class<? extends Channel> channelClass();

    /**
     * Returns the class of {@link DatagramChannel}
     *
     * @return the {@link Class} of {@link DatagramChannel}
     */
    Class<? extends Channel> datagramChannelClass();

    /**
     * Returns the class of {@link ServerChannel}.
     *
     * @return the {@link Class} of {@link ServerChannel}
     */
    Class<? extends ServerChannel> serverChannelClass();

    /**
     * Returns a new {@link IoHandlerFactory}.
     *
     * @return an {@code IoHandlerFactory}
     */
    IoHandlerFactory createIoHandlerFactory();

    /**
     * Create a new {@link IoEventLoopGroup} using the default number of threads.
     *
     * @return an {@code IoEventLoopGroup}
     */
    default IoEventLoopGroup createGroup() {
        return new MultiThreadIoEventLoopGroup(createIoHandlerFactory());
    }

    /**
     * Create a new {@link IoEventLoopGroup} using the specified number of
     * threads.
     *
     * @param nThreads the number of threads
     * @return an {@code IoEventLoopGroup}
     */
    default IoEventLoopGroup createGroup(int nThreads) {
        return new MultiThreadIoEventLoopGroup(nThreads, createIoHandlerFactory());
    }

    /**
     * Create a new {@link IoEventLoopGroup} using the specified number of
     * threads and the given {@link ThreadFactory}.
     *
     * @param nThreads      the number of threads
     * @param threadFactory the {@link ThreadFactory}
     * @return an {@code IoEventLoopGroup}
     */
    default IoEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, createIoHandlerFactory());
    }

}
