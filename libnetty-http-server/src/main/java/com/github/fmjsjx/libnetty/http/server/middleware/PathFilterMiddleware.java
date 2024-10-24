package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.ThreadLocalMatcher;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * Delegating a {@link Middleware} with a path filter.
 * <p>
 * The delegated {@link Middleware} only effective when the path filter returns
 * {@code true}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class PathFilterMiddleware implements Middleware {

    /**
     * Convert path prefixes to path filter.
     *
     * @param pathPrefixes the prefixes of HTTP paths
     * @return a {@code Predicate<String>}
     */
    public static final Predicate<String> toFilter(String... pathPrefixes) {
        return switch (pathPrefixes.length) {
            case 0 -> p -> true; // always returns true
            case 1 -> {
                ThreadLocalMatcher tlm = new ThreadLocalMatcher(toPattern(pathPrefixes[0]));
                yield p -> tlm.reset(p).matches();
            }
            default -> {
                ThreadLocalMatchers threadLocalMatchers = new ThreadLocalMatchers(
                        Arrays.stream(pathPrefixes).map(PathFilterMiddleware::toPattern).toArray(Pattern[]::new));
                yield p -> {
                    Matcher[] ms = threadLocalMatchers.get();
                    for (Matcher m : ms) {
                        if (m.reset(p).matches()) {
                            return true;
                        }
                    }
                    return false;
                };
            }
        };
    }

    static final Pattern toPattern(String pathPrefix) {
        if (!pathPrefix.startsWith("/")) {
            pathPrefix = "/" + pathPrefix;
        }
        if (pathPrefix.matches("^(.*)/+$")) {
            pathPrefix = pathPrefix.replaceFirst("^(.*[^/])/+$", "$1");
        }
        String regex = "^" + pathPrefix.replaceAll("/+", "/+") + "(/+.*)?$";
        return Pattern.compile(regex);
    }

    private static final class ThreadLocalMatchers extends FastThreadLocal<Matcher[]> {

        private final Pattern[] patterns;

        private ThreadLocalMatchers(Pattern... patterns) {
            this.patterns = patterns;
        }

        protected Matcher[] initialValue() {
            return Arrays.stream(patterns).map(p -> p.matcher("")).toArray(Matcher[]::new);
        }

    }

    private final Predicate<String> pathFilter;
    private final Middleware delegated;

    /**
     * Constructs a new {@link PathFilterMiddleware} with the specified
     * {@code pathFilter} and {@code delegated} {@link Middleware}.
     * 
     * @param pathFilter a {@link Predicate} filters the HTTP path
     * @param delegated  the delegated {@link Middleware}
     */
    public PathFilterMiddleware(Predicate<String> pathFilter, Middleware delegated) {
        this.pathFilter = Objects.requireNonNull(pathFilter, "pathFilter must not be null");
        this.delegated = Objects.requireNonNull(delegated, "delegated must not be null");
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        if (pathFilter.test(ctx.path())) {
            return delegated.apply(ctx, next);
        }
        return next.doNext(ctx);
    }

}
