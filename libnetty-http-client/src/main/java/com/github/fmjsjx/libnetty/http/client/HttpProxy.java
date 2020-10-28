package com.github.fmjsjx.libnetty.http.client;

import java.net.InetSocketAddress;

import com.github.fmjsjx.libnetty.http.HttpCommonUtil;

/**
 * Settings for HTTP proxy.
 * 
 * @since 1.2
 *
 * @author MJ Fang
 */
public class HttpProxy {

    private final InetSocketAddress address;
    private final CharSequence authorization;

    /**
     * Constructs a new {@link HttpProxy} with the specified host and port.
     * 
     * @param host the host
     * @param port the port
     */
    public HttpProxy(String host, int port) {
        this(InetSocketAddress.createUnresolved(host, port), null);
    }

    /**
     * Constructs a new {@link HttpProxy} with the specified host, port, user and
     * password.
     * 
     * @param host     the host
     * @param port     the port
     * @param user     the user
     * @param password the password
     */
    public HttpProxy(String host, int port, String user, String password) {
        this(InetSocketAddress.createUnresolved(host, port), HttpCommonUtil.basicAuthentication(user, password));
    }

    /**
     * Constructs a new {@link HttpProxy} with the specified address and
     * authorization.
     * 
     * @param address       the address
     * @param authorization the authorization
     */
    public HttpProxy(InetSocketAddress address, CharSequence authorization) {
        this.address = address;
        this.authorization = authorization;
    }

    /**
     * Returns the address of the HTTP proxy server.
     * 
     * @return the address of the HTTP proxy server
     */
    public InetSocketAddress address() {
        return address;
    }

    /**
     * Returns the authorization of the HTTP proxy.
     * 
     * @return the authorization of the HTTP proxy
     */
    public CharSequence authorization() {
        return authorization;
    }

    @Override
    public String toString() {
        return "ProxySettings[address=" + address + ", authorization=" + authorization + "]";
    }

}
