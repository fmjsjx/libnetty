package com.github.fmjsjx.libnetty.http.client;

import java.net.InetSocketAddress;
import java.util.Objects;

import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

/**
 * Implementations of {@link ProxyHandlerFactory}.
 * 
 * @since 1.2
 *
 * @author MJ Fang
 */
public class ProxyHandlerFactories {

    /**
     * Creates factory for {@link HttpProxyHandler}.
     * 
     * @param host the host of the proxy address
     * @param port the port of the proxy address
     * @return a {@code ProxyHandlerFactory<HttpProxyHandler>}
     */
    public static final ProxyHandlerFactory<HttpProxyHandler> forHttp(String host, int port) {
        return new HttpProxyHandlerFactory(host, port);
    }

    /**
     * Creates factory for {@link HttpProxyHandler}.
     * 
     * @param host     the host of the proxy address
     * @param port     the port of the proxy address
     * @param username the user name
     * @param password the password
     * @return a {@code ProxyHandlerFactory<HttpProxyHandler>}
     */
    public static final ProxyHandlerFactory<HttpProxyHandler> forHttp(String host, int port, String username,
            String password) {
        return new HttpProxyHandlerFactory(host, port, username, password);
    }

    /**
     * Creates factory for {@link Socks5ProxyHandler}.
     * 
     * @param host the host of the proxy address
     * @param port the port of the proxy address
     * @return a {@code ProxyHandlerFactory<Socks5ProxyHandler>}
     */
    public static final ProxyHandlerFactory<Socks5ProxyHandler> forSocks5(String host, int port) {
        return new Socks5ProxyHandlerFactory(host, port);
    }

    /**
     * Creates factory for {@link Socks5ProxyHandler}.
     * 
     * @param host     the host of the proxy address
     * @param port     the port of the proxy address
     * @param username the user name
     * @param password the password
     * @return a {@code ProxyHandlerFactory<Socks5ProxyHandler>}
     */
    public static final ProxyHandlerFactory<Socks5ProxyHandler> forSocks5(String host, int port, String username,
            String password) {
        return new Socks5ProxyHandlerFactory(host, port, username, password);
    }

    /**
     * Creates factory for {@link Socks4ProxyHandler}.
     * 
     * @param host the host of the proxy address
     * @param port the port of the proxy address
     * @return a {@code ProxyHandlerFactory<Socks5ProxyHandler>}
     */
    public static final ProxyHandlerFactory<Socks4ProxyHandler> forSocks4(String host, int port) {
        return new Socks4ProxyHandlerFactory(host, port);
    }

    /**
     * Creates factory for {@link Socks4ProxyHandler}.
     * 
     * @param host     the host of the proxy address
     * @param port     the port of the proxy address
     * @param username the user name
     * @return a {@code ProxyHandlerFactory<Socks4ProxyHandler>}
     */
    public static final ProxyHandlerFactory<Socks4ProxyHandler> forSocks5(String host, int port, String username) {
        return new Socks4ProxyHandlerFactory(host, port, username);
    }

    private ProxyHandlerFactories() {
    }

}

class ProxyCredentials {

    private final String username;
    private final String password;

    ProxyCredentials(String username) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = null;
    }

    ProxyCredentials(String username, String password) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "ProxyCredentials[username=" + username + ", password=" + password + "]";
    }

}

abstract class AbstractProxyFactory<T extends ProxyHandler> implements ProxyHandlerFactory<T> {

    protected final InetSocketAddress proxyAddress;
    protected final ProxyCredentials proxyCredentials;

    protected AbstractProxyFactory(String host, int port, ProxyCredentials proxyCredentials) {
        this(new InetSocketAddress(host, port), proxyCredentials);
    }

    private AbstractProxyFactory(InetSocketAddress proxyAddress, ProxyCredentials proxyCredentials) {
        this.proxyAddress = Objects.requireNonNull(proxyAddress, "proxyAddress must not be null");
        this.proxyCredentials = proxyCredentials;
    }

}

class HttpProxyHandlerFactory extends AbstractProxyFactory<HttpProxyHandler> {

    HttpProxyHandlerFactory(String host, int port, String username, String password) {
        super(host, port, new ProxyCredentials(username, password));
    }

    HttpProxyHandlerFactory(String host, int port) {
        super(host, port, null);
    }

    @Override
    public HttpProxyHandler create() {
        ProxyCredentials credentials = proxyCredentials;
        if (credentials == null) {
            return new HttpProxyHandler(proxyAddress);
        }
        return new HttpProxyHandler(proxyAddress, credentials.username(), credentials.password());
    }
}

class Socks5ProxyHandlerFactory extends AbstractProxyFactory<Socks5ProxyHandler> {

    Socks5ProxyHandlerFactory(String host, int port, String username, String password) {
        super(host, port, new ProxyCredentials(username, password));
    }

    Socks5ProxyHandlerFactory(String host, int port) {
        super(host, port, null);
    }

    @Override
    public Socks5ProxyHandler create() {
        ProxyCredentials credentials = proxyCredentials;
        if (credentials == null) {
            return new Socks5ProxyHandler(proxyAddress);
        }
        return new Socks5ProxyHandler(proxyAddress, credentials.username(), credentials.password());
    }

}

class Socks4ProxyHandlerFactory extends AbstractProxyFactory<Socks4ProxyHandler> {

    Socks4ProxyHandlerFactory(String host, int port, String username) {
        super(host, port, new ProxyCredentials(username));
    }

    Socks4ProxyHandlerFactory(String host, int port) {
        super(host, port, null);
    }

    @Override
    public Socks4ProxyHandler create() {
        ProxyCredentials credentials = proxyCredentials;
        if (credentials == null) {
            return new Socks4ProxyHandler(proxyAddress);
        }
        return new Socks4ProxyHandler(proxyAddress, credentials.username());
    }

}
