package com.github.fmjsjx.libnetty.transport;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.*;

/**
 * Interface for netty transport library.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
public interface TransportLibrary {

    /**
     * Returns default {@link TransportLibrary} instance, native library is
     * preferred.
     * 
     * @return the default {@link TransportLibrary}
     */
    static TransportLibrary getDefault() {
        return TransportLibraries.getDefault();
    }

    /**
     * Returns the class of {@link Channel}.
     * 
     * @return the {@link Class} of {@link Channel}
     */
    Class<? extends Channel> channelClass();

    /**
     * Returns the class of {@link ServerChannel}.
     * 
     * @return the {@link Class} of {@link ServerChannel}
     */
    Class<? extends ServerChannel> serverChannelClass();

    /**
     * Create a new {@link EventLoopGroup} instance using the default number of
     * threads.
     * 
     * @return an {@link EventLoopGroup}
     * @deprecated Please use {@link #createIoGroup()} instead
     */
    @Deprecated
    default EventLoopGroup createGroup() {
        return createIoGroup();
    }

    /**
     * Create a new {@link EventLoopGroup} instance using the specified number of
     * threads.
     * 
     * @param nThreads the number of threads
     * @return an {@link EventLoopGroup}
     * @deprecated Please use {@link #createIoGroup(int)} instead
     */
    @Deprecated
    default EventLoopGroup createGroup(int nThreads) {
        return createIoGroup(nThreads);
    }

    /**
     * Create a new instance using the specified number of threads and the given
     * {@link ThreadFactory}.
     *
     * @param nThreads      the number of threads
     * @param threadFactory the {@link ThreadFactory}
     * @return an {@link EventLoopGroup}
     * @deprecated Please use {@link #createIoGroup(int, ThreadFactory)} instead.
     */
    @Deprecated
    default EventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return createIoGroup(nThreads, threadFactory);
    }

    /**
     * Create a new {@link IoEventLoopGroup} using the default number of threads.
     *
     * @return an {@code IoEventLoopGroup}
     * @since 3.8
     */
    default IoEventLoopGroup createIoGroup() {
        return new MultiThreadIoEventLoopGroup(createIoHandlerFactory());
    }

    /**
     * Create a new {@link IoEventLoopGroup} using the specified number of
     * threads.
     *
     * @param nThreads the number of threads
     * @return an {@code IoEventLoopGroup}
     * @since 3.8
     */
    default IoEventLoopGroup createIoGroup(int nThreads) {
        return new MultiThreadIoEventLoopGroup(nThreads, createIoHandlerFactory());
    }

    /**
     * Create a new {@link IoEventLoopGroup} using the specified number of
     * threads and the given {@link ThreadFactory}.
     *
     * @param nThreads      the number of threads
     * @param threadFactory the {@link ThreadFactory}
     * @return an {@code IoEventLoopGroup}
     * @since 3.8
     */
    default IoEventLoopGroup createIoGroup(int nThreads, ThreadFactory threadFactory) {
        return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, createIoHandlerFactory());
    }

    /**
     * Returns a new {@link IoHandlerFactory}.
     *
     * @return an {@code IoHandlerFactory}
     * @since 3.8
     */
    IoHandlerFactory createIoHandlerFactory();

}
