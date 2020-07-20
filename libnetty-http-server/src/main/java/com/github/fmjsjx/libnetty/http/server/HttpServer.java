package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.socket.ServerSocketChannel;

/**
 * An interface defines an HTTP server.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface HttpServer {

    /**
     * Returns the display name of this server.
     * 
     * @return the display name of this server
     */
    String name();

    /**
     * Returns if this server is {@code running} or not.
     * 
     * @return {@code true} if this server is {@code running}
     */
    boolean isRunning();

    /**
     * Start up this server.
     * 
     * @return this server
     * @throws Exception if any error occurs
     */
    HttpServer startup() throws Exception;

    /**
     * Returns the binding {@link ServerSocketChannel}.
     * 
     * @return a {@code ServerSocketChannel}
     */
    ServerSocketChannel channel();

    /**
     * Shut down this server.
     * 
     * @return this server
     * @throws Exception if any error occurs
     */
    HttpServer shutdown() throws Exception;

}
