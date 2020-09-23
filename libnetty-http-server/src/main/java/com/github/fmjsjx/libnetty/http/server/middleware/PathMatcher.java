package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.List;
import java.util.regex.Matcher;

import com.github.fmjsjx.libnetty.http.server.DefaultPathVariables;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.PathPattern;
import com.github.fmjsjx.libnetty.http.server.PathPatternUtil;
import com.github.fmjsjx.libnetty.http.server.PathVariables;

/**
 * An interface defines matching methods for HTTP path.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@FunctionalInterface
public interface PathMatcher {

    /**
     * Returns a new {@link PathMatcher} from specified path pattern
     * 
     * @param pathPattern the path pattern string
     * @return a {@code PathMatcher}
     */
    static PathMatcher fromPattern(String pathPattern) {
        return from(PathPatternUtil.build(pathPattern));
    }

    /**
     * Returns a new {@link PathMatcher} from specified {@link PathPattern}.
     * 
     * @param pathPattern the path pattern
     * @return a {@code PathMatcher}
     */
    static PathMatcher from(PathPattern pathPattern) {
        return () -> pathPattern;
    }

    /**
     * Check if the path of the specified {@link HttpRequestContext} matches this
     * matcher or not.
     * 
     * @param ctx the context of the HTTP request
     * @return {@code true} if the path of the specified
     *         {@link HttpRequestContext}matches this matcher, {@code false}
     *         otherwise
     */
    default boolean matches(HttpRequestContext ctx) {
        PathPattern pattern = pathPattern();
        Matcher matcher = pattern.matcher(ctx.path());
        if (matcher.matches()) {
            List<String> pathVariableNames = pattern.pathVariableNames();
            if (pathVariableNames.isEmpty()) {
                ctx.pathVariables(PathVariables.EMPTY);
            } else {
                DefaultPathVariables pathVariables = new DefaultPathVariables();
                for (String name : pathVariableNames) {
                    String value = matcher.group(name);
                    pathVariables.put(name, value);
                }
                ctx.pathVariables(pathVariables);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the path pattern.
     * 
     * @return a {@code PathPattern}
     */
    PathPattern pathPattern();

}
