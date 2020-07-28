package com.github.fmjsjx.libnetty.http.server;

/**
 * An interface defines matching methods for HTTP APIs.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface ApiMatcher {

    /**
     * Check if the specified {@link HttpRequestContext} matches this matcher or
     * not.
     * 
     * @param ctx the {@link HttpRequestContext}
     * 
     * @return {@code true} if the specified {@code HttpRequestContext} matches this
     *         matcher
     */
    boolean matches(HttpRequestContext ctx);

    /**
     * Returns the path variable string at the specified {@code position}.
     * 
     * @param pos the position of the path variable begin with {@code 1}
     * 
     * @return the path variable string
     * 
     * @throws IllegalStateException     if last {@link HttpRequestContext} didn't
     *                                   match this matcher
     * @throws IndexOutOfBoundsException if there is no variable in the path with
     *                                   the given {@code position}
     */
    String pathVariable(int pos) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Returns the path variable string at the specified {@code name}.
     * 
     * @param name the name of the path variable
     * 
     * @return the path variable string
     * 
     * @throws IllegalStateException    if last {@link HttpRequestContext} didn't
     *                                  match this matcher
     * @throws IllegalArgumentException if there is no variable in the path with the
     *                                  given {@code name}
     */
    String pathVariable(String name) throws IllegalStateException, IllegalArgumentException;

}
