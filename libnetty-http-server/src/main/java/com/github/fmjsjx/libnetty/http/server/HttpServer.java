package com.github.fmjsjx.libnetty.http.server;

import io.netty.channel.ServerChannel;

/**
 * An interface defines an HTTP server.
 *
 * @author MJ Fang
 * @since 1.1
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
     * Returns the binding {@link ServerChannel}.
     *
     * @return a {@code ServerChannel}
     */
    ServerChannel channel();

    /**
     * Shut down this server.
     *
     * @return this server
     * @throws Exception if any error occurs
     */
    HttpServer shutdown() throws Exception;

    /**
     * Returns whether to enable SSL support.
     *
     * @return {@code true} if is enabled SSL support
     */
    boolean isSslEnabled();

    /**
     * Returns whether to enable HTTP/2 support.
     *
     * @return {@code true} if is enabled HTTP/2 support
     * @since 4.1
     */
    boolean isHttp2Enabled();

    /**
     * An interface defines a user access to the HTTP server.
     *
     * @author MJ Fang
     * @since 1.1
     */
    interface User {

        /**
         * The key of the {@link User} used in HTTP request context properties.
         * <p>
         * The value is
         * {@code com.github.fmjsjx.libnetty.http.server.HttpServer.User.class}.
         */
        Class<User> KEY = User.class;

        /**
         * Returns the username of this user.
         *
         * @return the username of this user
         */
        String username();

    }

    /**
     * The abstract implementation of {@link User}.
     *
     * @author MJ Fang
     * @since 1.1
     */
    abstract class AbstractUser implements User {

        private final String username;

        protected AbstractUser(String username) {
            this.username = username;
        }

        @Override
        public String username() {
            return username;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[username=" + username + "]";
        }

    }

}
