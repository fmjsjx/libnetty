package com.github.fmjsjx.libnetty.transport;

import java.nio.channels.Selector;
import java.util.concurrent.ThreadFactory;

import com.github.fmjsjx.libnetty.transport.io.NioIoTransportLibrary;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * {@link TransportLibrary} implementations which is used for NIO
 * {@link Selector} based {@link Channel}s.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 * @deprecated since 3.8, please use {@link NioIoTransportLibrary} instead
 */
@Deprecated
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
    public NioEventLoopGroup createGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    public NioEventLoopGroup createGroup(int nThreads) {
        return new NioEventLoopGroup(nThreads);
    }

    @Override
    public NioEventLoopGroup createGroup(int nThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(nThreads, threadFactory);
    }

}
