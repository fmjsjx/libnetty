package com.github.fmjsjx.libnetty.http.server;

import java.util.List;
import java.util.regex.Pattern;

/**
 * An interface defines methods for HTTP path pattern.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface PathPattern {

    /**
     * Build a new {@link PathPattern} from HTTP path pattern.
     * 
     * @param pathPattern the pattern of HTTP path
     * 
     * @return a {@code PathPattern}
     * 
     * @throws IllegalArgumentException if the path variable is illegal
     */
    static PathPattern build(String pathPattern) throws IllegalArgumentException {
        return PathPatternUtil.build(pathPattern);
    }

    /**
     * Returns the compiled representation of a regular expression.
     * 
     * @return the compiled representation of a regular expression
     */
    Pattern pattern();

    /**
     * Returns the list contains the names of path variables.
     * 
     * @return the list contains the names of path variables
     */
    List<String> pathVariableNames();

}
