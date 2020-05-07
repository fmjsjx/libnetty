package com.github.fmjsjx.libnetty.http.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for {@link SocketChannel}.
 * 
 * @since 1.0
 * 
 * @author fmjsjx
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SocketChannelUtil {

    /**
     * Returns the matching class of {@link SocketChannel} with the
     * {@link EventLoopGroup} given.
     * 
     * @param group the {@link EventLoopGroup}
     * @return the class of {@link SocketChannel}
     */
    public static Class<? extends SocketChannel> fromEventLoopGroup(EventLoopGroup group) {
        String className = group.getClass().getSimpleName();
        if (className.startsWith("Nio")) {
            return NioSocketChannel.class;
        } else if (className.startsWith("Epoll")) {
            return io.netty.channel.epoll.EpollSocketChannel.class;
        } else if (className.startsWith("KQueue")) {
            return io.netty.channel.kqueue.KQueueSocketChannel.class;
        }
        throw new IllegalArgumentException("Cannot find matching SocketChannel class for " + group.getClass());
    }

}
