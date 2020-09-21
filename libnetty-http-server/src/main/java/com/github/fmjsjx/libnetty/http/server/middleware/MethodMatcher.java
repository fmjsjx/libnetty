package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;

import io.netty.handler.codec.http.HttpMethod;

/**
 * An interface defines matching methods for HTTP method.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface MethodMatcher extends Predicate<HttpMethod> {

    /**
     * Matches any method.
     * <p>
     * Always returns {@code true}.
     */
    static MethodMatcher ANY_METHOD = m -> true;

    /**
     * Returns the singleton matcher {@link #ANY_METHOD}.
     * 
     * @return the singleton matcher {@link #ANY_METHOD}
     */
    static MethodMatcher any() {
        return ANY_METHOD;
    }

    /**
     * Returns a matcher that checks if the specified method and coming method is
     * equal.
     * 
     * @param method an {@link HttpMethod}
     * @return a matcher that checks if the specified method and coming method is
     *         equal
     */
    static MethodMatcher eq(HttpMethod method) {
        return method::equals;
    }

    /**
     * Returns a matcher that checks if the coming method is equal to any one of the
     * specified methods.
     * 
     * @param methods an array of {@link HttpMethod}
     * @return a matcher that checks if the coming method is equal to any one of the
     *         specified methods
     */
    static MethodMatcher in(HttpMethod... methods) {
        switch (methods.length) {
        case 0:
            return m -> false;
        case 1:
            return eq(methods[0]);
        default:
            Set<HttpMethod> set = Arrays.stream(methods).collect(Collectors.toSet());
            return set::contains;
        }
    }

    /**
     * Check if the specified {@code method} matches this matcher or not.
     * 
     * @param method an {@link HttpMethod}
     * @return {@code true} if the specified {@code method} matches this matcher,
     *         {@code false} otherwise
     */
    boolean matches(HttpMethod method);

    /**
     * Check if the method of the specified {@link HttpRequestContext} matches this
     * matcher or not.
     * 
     * @param ctx the context of the HTTP request
     * @return {@code true} if the method of the specified
     *         {@link HttpRequestContext} matches this matcher, {@code false}
     *         otherwise
     */
    default boolean matches(HttpRequestContext ctx) {
        return matches(ctx.method());
    }

    /**
     * Evaluates this predicate on the specified {@code method}.
     * <p>
     * This method is equivalent to {@link #matches(HttpMethod)}.
     * 
     * @param method an {@link HttpMethod}
     * @return {@code true} if the specified {@code method} matches this matcher,
     *         {@code false} otherwise
     */
    @Override
    default boolean test(HttpMethod method) {
        return matches(method);
    }

}
