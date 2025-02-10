package com.github.fmjsjx.libnetty.transport;

import java.util.concurrent.ThreadFactory;

import com.github.fmjsjx.libnetty.transport.io.IoTransportLibrary;
import io.netty.channel.*;

/**
 * Interface for netty transport library.
 *
 * @author MJ Fang
 * @see IoTransportLibrary
 * @since 1.0
 * @deprecated since 3.8, please use {@link IoTransportLibrary} instead
 */
@Deprecated
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
     * @return a {@link EventLoopGroup}
     */
    EventLoopGroup createGroup();

    /**
     * Create a new {@link EventLoopGroup} instance using the specified number of
     * threads.
     *
     * @param nThreads the number of threads
     * @return a {@link EventLoopGroup}
     */
    EventLoopGroup createGroup(int nThreads);

    /**
     * Create a new instance using the specified number of threads and the given
     * {@link ThreadFactory}.
     *
     * @param nThreads      the number of threads
     * @param threadFactory the {@link ThreadFactory}
     * @return a {@link EventLoopGroup}
     */
    EventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory);

}
