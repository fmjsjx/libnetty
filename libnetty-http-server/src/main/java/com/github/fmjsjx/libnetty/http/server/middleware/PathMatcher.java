package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.List;
import java.util.regex.Matcher;

import com.github.fmjsjx.libnetty.http.server.DefaultPathVariables;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.PathPattern;
import com.github.fmjsjx.libnetty.http.server.PathVariables;

/**
 * An interface defines matching methods for HTTP path.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface PathMatcher {

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
                int group = 1;
                for (String name : pathVariableNames) {
                    String value = matcher.group(group);
                    pathVariables.put(name, value);
                    group++;
                }
                ctx.pathVariables(pathVariables);
            }
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
