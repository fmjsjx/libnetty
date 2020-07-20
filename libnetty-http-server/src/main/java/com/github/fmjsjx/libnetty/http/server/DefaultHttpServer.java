package com.github.fmjsjx.libnetty.http.server;

import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.socket.ServerSocketChannel;

/**
 * The default implementation of {@link HttpServer}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultHttpServer implements HttpServer {

    private String name;
    private final AtomicBoolean running = new AtomicBoolean();

    private ServerSocketChannel channel;

    @Override
    public String name() {
        return name;
    }

    /**
     * Set the name of this server.
     * 
     * @param name the name string
     * @return this server
     */
    public DefaultHttpServer name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public HttpServer startup() throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("The HTTP server '" + name + "' is already started!");
        }
        // TODO
        return this;
    }

    @Override
    public ServerSocketChannel channel() {
        return channel;
    }

    @Override
    public HttpServer shutdown() throws Exception {
        if (!running.compareAndSet(true, false)) {
            throw new IllegalStateException("The HTTP server '" + name + "' is not running!");
        }
        // TODO
        return this;
    }

}
