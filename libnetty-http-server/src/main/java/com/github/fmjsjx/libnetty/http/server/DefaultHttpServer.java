package com.github.fmjsjx.libnetty.http.server;

import static java.util.Objects.*;

import java.util.ArrayList;

import static io.netty.channel.ChannelOption.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.http.HttpContentCompressorFactory;
import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.transport.TransportLibrary;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * The default implementation of {@link HttpServer}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultHttpServer implements HttpServer {

    private static final String DEFAULT_NAME = "default";
    private static final int DEFAULT_PORT_HTTP = 80;
    private static final int DEFAULT_PORT_HTTPS = 443;

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpServer.class);

    private static final int DEFAULT_MAX_CONTENT_LENGTH = 1 * 1024 * 1024;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private String name;
    private String host;
    private int port;
    private int ioThreads;

    private final AtomicBoolean running = new AtomicBoolean();

    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;
    private volatile boolean closeGroupsWhenShutdown;
    private Class<? extends ServerChannel> channelClass;
    private ServerChannel channel;

    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
    private CorsConfig corsConfig;

    private SslContextProvider sslContextProvider;

    @SuppressWarnings("rawtypes")
    private Map<ChannelOption, Object> options = new LinkedHashMap<>();
    @SuppressWarnings("rawtypes")
    private Map<ChannelOption, Object> childOptions = new LinkedHashMap<>();

    private List<Consumer<HttpContentCompressorFactory.Builder>> compressionSettingsListeners = new ArrayList<>();
    private HttpContentCompressorFactory httpContentCompressorFactory;

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code name}
     * and {@code port}.
     * 
     * @param name the name of the server
     * @param port the port
     */
    public DefaultHttpServer(String name, int port) {
        name(name).port(port);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code port}.
     * 
     * @param port the port
     */
    public DefaultHttpServer(int port) {
        this(DEFAULT_NAME, port);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the default HTTP port
     * ({@code 80}).
     */
    public DefaultHttpServer() {
        this(DEFAULT_PORT_HTTP);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code name}
     * and default HTTP port ({@code 80}).
     * 
     * @param name the name of the server
     */
    public DefaultHttpServer(String name) {
        this(name, DEFAULT_PORT_HTTP);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified
     * {@code sslContextProvider} and default HTTPs port ({@code 443}).
     * 
     * @param sslContextProvider the {@code sslContextProvider}
     */
    public DefaultHttpServer(SslContextProvider sslContextProvider) {
        this(DEFAULT_NAME, sslContextProvider);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code name},
     * {@code sslContextProvider} and default HTTPs port ({@code 443}).
     * 
     * @param name               the name of the server
     * @param sslContextProvider the {@code sslContextProvider}
     */
    public DefaultHttpServer(String name, SslContextProvider sslContextProvider) {
        this(name, sslContextProvider, DEFAULT_PORT_HTTPS);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code name},
     * {@code sslContextProvider} and port.
     * 
     * @param name               the name of the server
     * @param sslContextProvider the {@code sslContextProvider}
     * @param port               the port
     */
    public DefaultHttpServer(String name, SslContextProvider sslContextProvider, int port) {
        this(name, port);
        enableSsl(sslContextProvider);
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Set the name of this server.
     * <p>
     * The default value is {@code "default"}.
     * 
     * @param name the name string
     * @return this server
     */
    public DefaultHttpServer name(String name) {
        ensureNotStarted();
        this.name = requireNonNull(name, "name must not be null");
        return this;
    }

    private void ensureNotStarted() {
        if (isRunning()) {
            throw alreadyStarted();
        }
    }

    private IllegalStateException alreadyStarted() {
        return new IllegalStateException("The HTTP server '" + name + "' is already started!");
    }

    /**
     * Returns the listening port of this server.
     * 
     * @return the port
     */
    public int port() {
        return port;
    }

    static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("port out of range:" + port);
        return port;
    }

    /**
     * Set the listening port of this server.
     * 
     * @param port the port
     * @return this server
     */
    public DefaultHttpServer port(int port) {
        ensureNotStarted();
        this.port = checkPort(port);
        return this;
    }

    /**
     * Returns the host name to which the server should bind.
     * 
     * @return the host name
     */
    public String host() {
        return host;
    }

    /**
     * Set the host name to which the server should bind.
     * <p>
     * The default value, {@code null}, means any address.
     * 
     * @param host the host name
     * @return this server
     */
    public DefaultHttpServer host(String host) {
        ensureNotStarted();
        this.host = host;
        return this;
    }

    /**
     * Returns the number of I/O threads to create for this server. When the value
     * is {@code 0}, the default, the number is derived from the number of available
     * processors x {@code 2}.
     * 
     * @return the number of I/O threads to create for this server
     */
    public int ioThreads() {
        return ioThreads;
    }

    /**
     * Set the number of I/O threads to create for this server.
     * 
     * @param ioThreads the number of I/O threads to create for this server
     * @return this server
     */
    public DefaultHttpServer ioThreads(int ioThreads) {
        ensureNotStarted();
        if (ioThreads < 0) {
            throw new IllegalArgumentException("ioThreads must not be negative");
        }
        this.ioThreads = ioThreads;
        return this;
    }

    /**
     * Specify the transport components of this server.
     * 
     * @param group        a {@code EventLoopGroup} used for both the parent
     *                     (acceptor) and the child (client)
     * @param channelClass a {@code Class} which is used to create {@code Channel}
     *                     instances
     * @return this server
     */
    public DefaultHttpServer transport(EventLoopGroup group, Class<? extends ServerSocketChannel> channelClass) {
        return transport(group, group, channelClass);
    }

    /**
     * Specify the transport components of this server.
     * 
     * @param parentGroup  a {@code EventLoopGroup} used for the parent (acceptor)
     * @param childGroup   a {@code EventLoopGroup} used for the child (client)
     * @param channelClass a {@code Class} which is used to create {@code Channel}
     *                     instances
     * @return this server
     */
    public DefaultHttpServer transport(EventLoopGroup parentGroup, EventLoopGroup childGroup,
            Class<? extends ServerSocketChannel> channelClass) {
        ensureNotStarted();
        this.parentGroup = requireNonNull(parentGroup, "parentGroup must not be null");
        this.childGroup = requireNonNull(childGroup, "childGroup must not be null");
        this.channelClass = requireNonNull(channelClass, "channelClass must not be null");
        return this;
    }

    /**
     * Set the {@link CorsConfig} of this server.
     * <p>
     * The default value, {@code null}, means this server does not allow cross
     * domain.
     * 
     * @param corsConfig the {@code CorsConfig} to be set
     * @return this server
     */
    public DefaultHttpServer corsConfig(CorsConfig corsConfig) {
        ensureNotStarted();
        this.corsConfig = corsConfig;
        return this;
    }

    /**
     * Returns the maximum length of HTTP content.
     * 
     * @return the maximum length of HTTP content
     */
    public int maxContentLength() {
        return maxContentLength;
    }

    /**
     * Set the maximum length of HTTP content.
     * <p>
     * The default value is {@code 1048576(1MB)}.
     * 
     * @param maxContentLength the maximum length of HTTP content
     * @return this server
     */
    public DefaultHttpServer maxContentLength(int maxContentLength) {
        ensureNotStarted();
        this.maxContentLength = maxContentLength;
        return this;
    }

    /**
     * Returns the time in seconds that connectors wait for another HTTP request
     * before closing the connection.
     * 
     * @return the time in seconds
     */
    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Set the time in seconds that connectors wait for another HTTP request before
     * closing the connection.
     * <p>
     * The default value is {@code 60}.
     * 
     * @param timeoutSeconds time in seconds
     * @return this server
     */
    public DefaultHttpServer timeoutSeconds(int timeoutSeconds) {
        ensureNotStarted();
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("timeoutSeconds must not be negative");
        }
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the
     * {@link Channel} instances once they got created. Use a value of {@code null}
     * to remove a previous set {@link ChannelOption}.
     *
     * @param <T>    the type of the value which is valid for the
     *               {@code ChannelOption}
     * @param option a {@code ChannelOption}
     * @param value  the value
     * 
     * @return this server
     * 
     * @see ChannelOption
     */
    public <T> DefaultHttpServer option(ChannelOption<T> option, T value) {
        ensureNotStarted();
        requireNonNull(option, "option must not be null");
        if (value == null) {
            options.remove(option);
        } else {
            options.put(option, value);
        }
        return this;
    }

    /**
     * Set {@code SO_BACKLOG}.
     * 
     * @param value the value
     * @return this server
     */
    public DefaultHttpServer soBackLog(int value) {
        option(SO_BACKLOG, value);
        return this;
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the
     * {@link Channel} instances once they get created (after the acceptor accepted
     * the {@link Channel}). Use a value of {@code null} to remove a previous set
     * {@link ChannelOption}.
     * 
     * @param <T>         the type of the value which is valid for the
     *                    {@code ChannelOption}
     * @param childOption a {@code ChannelOption}
     * @param value       the value
     * 
     * @return this server
     * 
     * @see ChannelOption
     */
    public <T> DefaultHttpServer childOption(ChannelOption<T> childOption, T value) {
        ensureNotStarted();
        requireNonNull(childOption, "childOption must not be null");
        if (value == null) {
            childOptions.remove(childOption);
        } else {
            childOptions.put(childOption, value);
        }
        return this;
    }

    /**
     * Enable {@code TCP_NODELAY} (disable/enable Nagle's algorithm).
     * 
     * @return this server
     */
    public DefaultHttpServer tcpNoDelay() {
        childOption(TCP_NODELAY, true);
        return this;
    }

    /**
     * Returns whether to enable SSL support.
     * 
     * @return {@code true} if is enabled SSL support
     */
    public boolean isSslEnabled() {
        return sslContextProvider != null;
    }

    /**
     * Enable SSL support and set the {@link SslContextProvider}.
     * 
     * @param sslContextProvider a {@code SslContextProvider}
     * 
     * @return this server
     */
    public DefaultHttpServer enableSsl(SslContextProvider sslContextProvider) {
        ensureNotStarted();
        this.sslContextProvider = requireNonNull(sslContextProvider, "sslContextProvider must not be null");
        return this;
    }

    /**
     * Enable HTTP content compression feature and apply compression settings.
     * 
     * @param action the apply action
     * @return this server
     */
    public DefaultHttpServer applyCompressionSettings(Consumer<HttpContentCompressorFactory.Builder> action) {
        compressionSettingsListeners.add(action);
        return this;
    }

    /**
     * Reset all settings of this server.
     * 
     * @return this server
     */
    public DefaultHttpServer reset() {
        ensureNotStarted();
        name = DEFAULT_NAME;
        host = null;
        port = DEFAULT_PORT_HTTP;

        ioThreads = 0;
        parentGroup = null;
        childGroup = null;
        channelClass = null;
        channel = null;

        timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
        corsConfig = null;

        sslContextProvider = null;

        options.clear();
        childOptions.clear();

        compressionSettingsListeners.clear();
        return this;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public HttpServer startup() throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw alreadyStarted();
        }
        try {
            initSettings();
            ServerBootstrap bootstrap = new ServerBootstrap().group(parentGroup, childGroup).channel(channelClass);
            options.forEach(bootstrap::option);
            childOptions.forEach(bootstrap::childOption);
            DefaultHttpServerChannelInitializer initializer = new DefaultHttpServerChannelInitializer(timeoutSeconds,
                    maxContentLength, corsConfig, sslContextProvider, httpContentCompressorFactory);
            // TODO

            bootstrap.childHandler(initializer);

            ChannelFuture channelFuture = bind(bootstrap).sync();
            if (channelFuture.cause() != null) {
                throw new HttpRuntimeException("HTTP server start failed!", channelFuture.cause());
            }

            channel = (ServerChannel) channelFuture.channel();

            log.info("HTTP server '{}' started at {}.", name, channel.localAddress());

            return this;
        } catch (Exception e) {
            running.set(false);
            if (closeGroupsWhenShutdown) {
                closeGroups();
            }
            if (e instanceof HttpRuntimeException) {
                throw e;
            }
            throw new HttpRuntimeException("HTTP server start failed!", e);
        }
    }

    private void initSettings() {
        if (parentGroup == null) {
            parentGroup = TransportLibrary.getDefault().createGroup(1, new DefaultThreadFactory("http-parent"));
            closeGroupsWhenShutdown = true;
        }
        if (childGroup == null) {
            childGroup = TransportLibrary.getDefault().createGroup(ioThreads, new DefaultThreadFactory("http-child"));
        }
        if (channelClass == null) {
            channelClass = TransportLibrary.getDefault().serverChannelClass();
        }
        // always set AUTO_READ to false
        // use AutoReadNextHandler to read next HTTP request on Keep-Alive connection
        childOptions.put(AUTO_READ, false);

        if (compressionSettingsListeners.size() > 0) {
            HttpContentCompressorFactory.Builder builder = HttpContentCompressorFactory.builder();
            compressionSettingsListeners.forEach(a -> a.accept(builder));
            httpContentCompressorFactory = builder.build();
        }
    }

    private ChannelFuture bind(ServerBootstrap bootstrap) {
        if (host == null) {
            return bootstrap.bind(port);
        }
        return bootstrap.bind(host, port);
    }

    @Override
    public ServerChannel channel() {
        return channel;
    }

    @Override
    public HttpServer shutdown() throws Exception {
        if (!running.compareAndSet(true, false)) {
            throw new IllegalStateException("The HTTP server '" + name + "' is not running!");
        }
        if (closeGroupsWhenShutdown) {
            closeGroups();
        }
        return this;
    }

    private void closeGroups() {
        log.debug("Close parent group: {}", parentGroup);
        parentGroup.shutdownGracefully();
        log.debug("Close child group: {}", childGroup);
        childGroup.shutdownGracefully();
    }

    @Override
    public String toString() {
        return "DefaultHttpServer(name=" + nameToString() + ", host=" + host + ", port=" + port + ")";

    }

    private String nameToString() {
        return name + (isSslEnabled() ? "[SSL]" : "");
    }

}
