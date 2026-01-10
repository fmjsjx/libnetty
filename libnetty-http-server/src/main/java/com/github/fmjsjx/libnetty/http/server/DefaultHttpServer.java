package com.github.fmjsjx.libnetty.http.server;

import static io.netty.channel.ChannelOption.AUTO_READ;
import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.TCP_NODELAY;
import static io.netty.handler.codec.http.HttpHeaderNames.SERVER;
import static java.util.Objects.requireNonNull;

import com.github.fmjsjx.libnetty.handler.ssl.ChannelSslInitializer;
import com.github.fmjsjx.libnetty.http.HttpContentCompressorProvider;
import com.github.fmjsjx.libnetty.http.exception.HttpRuntimeException;
import com.github.fmjsjx.libnetty.http.server.component.*;
import com.github.fmjsjx.libnetty.transport.io.IoTransportLibrary;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The default implementation of {@link HttpServer}.
 *
 * @author MJ Fang
 * @since 1.1
 */
public class DefaultHttpServer implements HttpServer {

    private static final String DEFAULT_NAME = "default";
    private static final int DEFAULT_PORT_HTTP = 80;
    private static final int DEFAULT_PORT_HTTPS = 443;

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpServer.class);

    private static final int DEFAULT_MAX_CONTENT_LENGTH = Integer.MAX_VALUE;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final boolean DEFAULT_HTTP2_ENABLED = false;

    private static final Consumer<HttpHeaders> defaultAddHeaders = headers -> headers.set(SERVER, "libnetty");

    private String name;
    private String host;
    private InetAddress address;
    private int port;
    private int ioThreads;
    private boolean http2Enabled = DEFAULT_HTTP2_ENABLED;

    private final AtomicBoolean running = new AtomicBoolean();

    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;
    private volatile boolean closeGroupsWhenShutdown;
    private Class<? extends ServerChannel> channelClass;
    private ServerChannel channel;

    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
    private CorsConfig corsConfig;

    private ChannelSslInitializer<Channel> channelSslInitializer;

    private ServerBootstrap bootstrap = new ServerBootstrap();

    private final List<Consumer<HttpContentCompressorProvider.Builder>> compressionOptionsListeners = new ArrayList<>();

    private HttpContentCompressorProvider httpContentCompressorProvider;

    private HttpServerHandlerProvider handlerProvider;

    private final Map<Class<?>, HttpServerComponent> components = new LinkedHashMap<>();
    private Consumer<HttpHeaders> addHeaders = defaultAddHeaders;

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
     * {@code channelSslInitializer} and default HTTPs port ({@code 443}).
     *
     * @param channelSslInitializer the {@code ChannelSslInitializer}
     * @since 2.4
     */
    public DefaultHttpServer(ChannelSslInitializer<Channel> channelSslInitializer) {
        this(DEFAULT_NAME, channelSslInitializer);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code name},
     * {@code channelSslInitializer} and default HTTPs port ({@code 443}).
     *
     * @param name                  the name of the server
     * @param channelSslInitializer the {@code ChannelSslInitializer}
     * @since 2.4
     */
    public DefaultHttpServer(String name, ChannelSslInitializer<Channel> channelSslInitializer) {
        this(name, channelSslInitializer, DEFAULT_PORT_HTTPS);
    }

    /**
     * Constructs a new {@link DefaultHttpServer} with the specified {@code name},
     * {@code channelSslInitializer} and port.
     *
     * @param name                  the name of the server
     * @param channelSslInitializer the {@code ChannelSslInitializer}
     * @param port                  the port
     * @since 2.4
     */
    public DefaultHttpServer(String name, ChannelSslInitializer<Channel> channelSslInitializer, int port) {
        this(name, port);
        enableSsl(channelSslInitializer);
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
        this.address = null;
        return this;
    }

    /**
     * Returns the network address to which the server should bind.
     *
     * @return the network address to which the server should bind
     */
    public InetAddress address() {
        return address;
    }

    /**
     * Set the network address to which the server should bind
     * <p>
     * The default value, {@code null}, means any address.
     *
     * @param address the network address
     * @return this server
     */
    public DefaultHttpServer address(InetAddress address) {
        ensureNotStarted();
        this.address = address;
        this.host = null;
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
    public DefaultHttpServer transport(EventLoopGroup group, Class<? extends ServerChannel> channelClass) {
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
                                       Class<? extends ServerChannel> channelClass) {
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
     * The default value is {@code 2147483647(Integer.MAX_VALUE)}.
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
     * Set the time that connectors wait for another HTTP request before closing the
     * connection.
     * <p>
     * The default value is {@code 60} seconds.
     *
     * @param timeout time as {@link Duration} type
     * @return this server
     */
    public DefaultHttpServer timeout(Duration timeout) {
        ensureNotStarted();
        this.timeoutSeconds = (int) timeout.getSeconds();
        return this;
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
     * Let connections never timeout.
     * <p>
     * This method is equivalent to:
     *
     * <pre>
     * {@code
     *     timeoutSeconds(0);
     * }
     *
     * or
     *
     * {@code
     *     timeout(Duration.ZERO);
     * }
     * </pre>
     *
     * @return this server
     */
    public DefaultHttpServer neverTimeout() {
        return timeoutSeconds(0);
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
     * @return this server
     * @see ChannelOption
     */
    public <T> DefaultHttpServer option(ChannelOption<T> option, T value) {
        ensureNotStarted();
        requireNonNull(option, "option must not be null");
        bootstrap.option(option, value);
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
     * @return this server
     * @see ChannelOption
     */
    public <T> DefaultHttpServer childOption(ChannelOption<T> childOption, T value) {
        ensureNotStarted();
        requireNonNull(childOption, "childOption must not be null");
        bootstrap.childOption(childOption, value);
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

    @Override
    public boolean isSslEnabled() {
        return channelSslInitializer != null;
    }

    /**
     * Enable SSL support and set the {@link ChannelSslInitializer}
     *
     * @param channelSslInitializer a {@code ChannelSslInitializer}
     * @return this server
     * @since 2.4
     */
    public DefaultHttpServer enableSsl(ChannelSslInitializer<Channel> channelSslInitializer) {
        ensureNotStarted();
        requireNonNull(channelSslInitializer, "channelSslInitializer must not be null");
        this.channelSslInitializer = channelSslInitializer;
        return this;
    }

    /**
     * Disable SSL support.
     *
     * @return this server
     */
    public DefaultHttpServer disableSsl() {
        ensureNotStarted();
        this.channelSslInitializer = null;
        return this;
    }

    @Override
    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    /**
     * Enable/disable HTTP2 support.
     *
     * @param http2Enabled whether to enable HTTP2 or not
     * @return this server
     * @since 4.1
     */
    public DefaultHttpServer http2Enabled(boolean http2Enabled) {
        ensureNotStarted();
        this.http2Enabled = http2Enabled;
        return this;
    }

    /**
     * Enable HTTP2 support.
     *
     * @return this server
     * @since 4.1
     */
    public DefaultHttpServer enableHttp2() {
        return http2Enabled(true);
    }

    /**
     * Disable HTTP2 support.
     *
     * @return this server
     * @since 4.1
     */
    public DefaultHttpServer disableHttp2() {
        return http2Enabled(false);
    }

    /**
     * Enable HTTP content compression feature and apply compression options.
     *
     * @param action the apply action
     * @return this server
     */
    public DefaultHttpServer applyCompressionOptions(Consumer<HttpContentCompressorProvider.Builder> action) {
        ensureNotStarted();
        compressionOptionsListeners.add(action);
        return this;
    }

    /**
     * Set the singleton {@link HttpServerHandler} of this server.
     *
     * <p>
     * To use singleton handler, the implementation of {@link HttpServerHandler}
     * must be {@link Sharable}.
     *
     * @param handler an {@code HttpServerHandler}
     * @return this server
     */
    public DefaultHttpServer handler(HttpServerHandler handler) {
        ensureNotStarted();
        requireNonNull(handler, "handler must not be null");
        if (!handler.isSharable()) {
            throw new IllegalArgumentException("singleton handler must be sharable");
        }
        this.handlerProvider = () -> handler;
        return this;
    }

    /**
     * Set the {@link HttpServerHandlerProvider} of this server.
     *
     * @param handlerProvider an {@code HttpServerHandlerProvider}
     * @return this server
     */
    public DefaultHttpServer handlerProvider(HttpServerHandlerProvider handlerProvider) {
        ensureNotStarted();
        this.handlerProvider = requireNonNull(handlerProvider, "handlerProvider must not be null");
        return this;
    }

    /**
     * Use and returns the {@link DefaultHttpServerHandlerProvider} for this server.
     *
     * @return the {@code DefaultHttpServerHandlerProvider}
     */
    public DefaultHttpServerHandlerProvider defaultHandlerProvider() {
        ensureNotStarted();
        return (DefaultHttpServerHandlerProvider) (handlerProvider = new DefaultHttpServerHandlerProvider());
    }

    /**
     * Set an {@link HttpServerComponent}.
     *
     * @param component the component
     * @return this server
     * @since 1.3
     */
    public DefaultHttpServer component(HttpServerComponent component) {
        switch (component) {
            case JsonLibrary jsonLibrary -> components.put(JsonLibrary.class, jsonLibrary);
            case WorkerPool workerPool -> components.put(WorkerPool.class, workerPool);
            case ExceptionHandler exceptionHandler -> components.put(ExceptionHandler.class, exceptionHandler);
            default -> components.put(component.componentType(), component);
        }
        return this;
    }

    /**
     * Support JSON features.
     * <p>
     * This method is equivalent to: {@code component(JsonLibrary.getInstance())}.
     *
     * @return this server
     * @since 1.3
     */
    public DefaultHttpServer supportJson() {
        return component(JsonLibrary.getInstance());
    }

    /**
     * Use virtual thread worker pool with the default pool name.
     *
     * @return this server
     * @see #virtualWorkerPool(String)
     * @since 4.0
     */
    public DefaultHttpServer virtualWorkerPool() {
        return virtualWorkerPool(null);
    }

    /**
     * Use virtual thread worker pool with the specified pool name given.
     *
     * @param poolName the pool name
     * @return this server
     * @see #virtualWorkerPool()
     * @since 4.0
     */
    public DefaultHttpServer virtualWorkerPool(String poolName) {
        var workerPool = poolName == null ? new VirtualThreadWorkerPool() : new VirtualThreadWorkerPool(poolName);
        return component(workerPool);
    }

    /**
     * Set the function to add HTTP response headers. Include default headers.
     * <p>
     * The default headers:
     *
     * <pre>
     * {@code server: libnetty}
     * </pre>
     *
     * @param addHeaders the function to add HTTP response headers
     * @return this server
     */
    public DefaultHttpServer addHeaders(Consumer<HttpHeaders> addHeaders) {
        return addHeaders(addHeaders, false);
    }

    /**
     * Set the function to add HTTP response headers.
     * <p>
     * The default headers:
     *
     * <pre>
     * {@code server: libnetty}
     * </pre>
     *
     * @param addHeaders the function to add HTTP response headers
     * @param force      if {@code true} then only invoke given function, if
     *                   {@code false} then will invoke both default function and
     *                   given function
     * @return this server
     */
    public DefaultHttpServer addHeaders(Consumer<HttpHeaders> addHeaders, boolean force) {
        ensureNotStarted();
        requireNonNull(addHeaders, "addHeaders must not be null");
        if (force) {
            this.addHeaders = addHeaders;
        } else {
            this.addHeaders = defaultAddHeaders.andThen(addHeaders);
        }
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
        address = null;
        port = DEFAULT_PORT_HTTP;

        ioThreads = 0;
        parentGroup = null;
        childGroup = null;
        channelClass = null;
        channel = null;

        timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
        corsConfig = null;

        channelSslInitializer = null;

        bootstrap = new ServerBootstrap();

        compressionOptionsListeners.clear();

        handlerProvider = null;

        components.clear();
        addHeaders = defaultAddHeaders;
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
            ServerBootstrap bootstrap = this.bootstrap;
            bootstrap.group(parentGroup, childGroup).channel(channelClass);
            Map<Class<?>, Object> components = this.components.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> Optional.ofNullable(e.getValue())));
            var initializer = createChannelInitializer(components);

            bootstrap.childHandler(initializer);

            ChannelFuture channelFuture = bind(bootstrap).sync();
            if (channelFuture.cause() != null) {
                throw new HttpRuntimeException("HTTP server start failed!", channelFuture.cause());
            }

            channel = (ServerChannel) channelFuture.channel();
            var localAddress = channel.localAddress();
            if (localAddress instanceof InetSocketAddress localSocketAddress) {
                port = localSocketAddress.getPort();
            }
            log.info("HTTP server '{}' started at {}.", name, localAddress);

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

    private ChannelInitializer<? extends Channel> createChannelInitializer(Map<Class<?>, Object> components) {
        if (isHttp2Enabled()) {
            log.debug("HTTP2 enabled, create and return HTTP2 server channel initializer.");
            // TODO create and returns Http2ServerChannelInitializer.
        }
        return new DefaultHttpServerChannelInitializer(timeoutSeconds, maxContentLength, corsConfig,
                channelSslInitializer(), httpContentCompressorProvider, handlerProvider, components, addHeaders);
    }

    private void initSettings() {
        if (handlerProvider == null) {
            throw new IllegalArgumentException("missing handlerProvider for HTTP server '" + name + "'");
        }
        if (parentGroup == null) {
            parentGroup = IoTransportLibrary.getDefault().createGroup(1, new DefaultThreadFactory("http-parent"));
            closeGroupsWhenShutdown = true;
        }
        if (childGroup == null) {
            childGroup = IoTransportLibrary.getDefault().createGroup(ioThreads, new DefaultThreadFactory("http-child"));
        }
        if (channelClass == null) {
            channelClass = IoTransportLibrary.getDefault().serverChannelClass();
        }
        // always set AUTO_READ to false
        // use AutoReadNextHandler to read next HTTP request on Keep-Alive connection
        bootstrap.childOption(AUTO_READ, false);
        if (!compressionOptionsListeners.isEmpty()) {
            var builder = HttpContentCompressorProvider.builder();
            compressionOptionsListeners.forEach(a -> a.accept(builder));
            httpContentCompressorProvider = builder.build();
        }
    }

    private ChannelSslInitializer<Channel> channelSslInitializer() {
        return channelSslInitializer;
    }

    private ChannelFuture bind(ServerBootstrap bootstrap) {
        if (address != null) {
            return bootstrap.bind(address, port);
        }
        if (host != null) {
            return bootstrap.bind(host, port);
        }
        return bootstrap.bind(port);
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
        HttpServerHandlerProvider handlerProvider = this.handlerProvider;
        log.debug("Close handler provider: {}", handlerProvider);
        handlerProvider.close();
        for (HttpServerComponent component : components.values()) {
            log.debug("Close component: {}", component);
            component.onServerClosed();
        }
        if (closeGroupsWhenShutdown) {
            closeGroups();
        }
        return this;
    }

    private void closeGroups() {
        EventLoopGroup parentGroup = this.parentGroup;
        log.debug("Close parent group: {}", parentGroup);
        parentGroup.shutdownGracefully();
        EventLoopGroup childGroup = this.childGroup;
        log.debug("Close child group: {}", childGroup);
        childGroup.shutdownGracefully();
    }

    @Override
    public String toString() {
        return "DefaultHttpServer(name=" + nameToString() + ", binding=" + bindingToString() + ")";
    }

    private String nameToString() {
        return name + (isSslEnabled() ? "[SSL]" : "");
    }

    private String bindingToString() {
        if (address != null) {
            return address + ":" + port;
        } else if (host != null) {
            return host + ":" + port;
        } else {
            return "*:" + port;
        }
    }

}
