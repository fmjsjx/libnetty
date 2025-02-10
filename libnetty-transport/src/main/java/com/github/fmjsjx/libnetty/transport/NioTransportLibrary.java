package com.github.fmjsjx.libnetty.transport;

import java.nio.channels.Selector;
import java.util.concurrent.ThreadFactory;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * {@link TransportLibrary} implementations which is used for NIO
 * {@link Selector} based {@link Channel}s.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */
@SuppressWarnings("deprecation")
public class NioTransportLibrary implements TransportLibrary {

    private static final class InstanceHolder {
        private static final NioTransportLibrary instance = new NioTransportLibrary();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return the singleton {@link NioTransportLibrary} instance
     */
    public static final NioTransportLibrary getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Class<NioSocketChannel> channelClass() {
        return NioSocketChannel.class;
    }

    @Override
    public Class<NioServerSocketChannel> serverChannelClass() {
        return NioServerSocketChannel.class;
    }

    @Override
    @Deprecated
    public NioEventLoopGroup createGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    @Deprecated
    public NioEventLoopGroup createGroup(int nThreads) {
        return new NioEventLoopGroup(nThreads);
    }

    @Override
    @Deprecated
    public NioEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(nThreads, threadFactory);
    }

    /**
     * Returns a new {@link IoHandlerFactory} that creates {@link NioIoHandler}
     * instances.
     */
    @Override
    public IoHandlerFactory createIoHandlerFactory() {
        return NioIoHandler.newFactory();
    }

}
