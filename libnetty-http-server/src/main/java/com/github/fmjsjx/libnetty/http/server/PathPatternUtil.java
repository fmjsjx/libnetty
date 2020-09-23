package com.github.fmjsjx.libnetty.http.server;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for HTTP path patterns.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class PathPatternUtil {

    private static final Logger log = LoggerFactory.getLogger(PathPatternUtil.class);

    private static final Pattern pathVariablePattern = Pattern.compile("\\{[A-Za-z]\\w*\\}");
    private static final Pattern anyPathVariablePattern = Pattern.compile("\\{.+\\}");

    /**
     * Build a new {@link PathPattern} from HTTP path pattern.
     * 
     * @param pathPattern        the pattern of HTTP path
     * @param threadLocalMatcher if matchers will be cached in a {@link ThreadLocal}
     *                           variable
     * @return a {@code PathPattern}
     * 
     * @throws IllegalArgumentException if the path variable is illegal
     */
    public static final PathPattern build(String pathPattern, boolean threadLocalMatcher)
            throws IllegalArgumentException {
        ArrayList<String> pathVariableNames = new ArrayList<String>();
        Pattern pattern = toPattern(pathPattern, pathVariableNames);
        return threadLocalMatcher ? new ThreadLocalMatcherPathPattern(pattern, pathVariableNames)
                : new BasicPathPattern(pattern, pathVariableNames);
    }

    /**
     * Build a new {@link PathPattern} from HTTP path pattern.
     * 
     * @param pathPattern the pattern of HTTP path
     * 
     * @return a {@code PathPattern}
     * 
     * @throws IllegalArgumentException if the path variable is illegal
     */
    public static final PathPattern build(String pathPattern) throws IllegalArgumentException {
        return build(pathPattern, true);
    }

    private static Pattern toPattern(String pathPattern, ArrayList<String> pathVariableNames) {
        if (!pathPattern.startsWith("/")) {
            pathPattern = "/" + pathPattern;
        }
        if (pathPattern.endsWith("/")) {
            pathPattern = pathPattern.substring(0, pathPattern.length() - 1);
        }
        String base = pathPattern.replace("-", "\\-").replace(".", "\\.").replace("/", "/+");
        Matcher m = pathVariablePattern.matcher(base);
        StringBuilder regexBuilder = new StringBuilder().append("^");
        int start = 0;
        for (; m.find(start); start = m.end()) {
            regexBuilder.append(base.substring(start, m.start()));
            String g = m.group();
            String name = g.substring(1, g.length() - 1);
            pathVariableNames.add(name);
            regexBuilder.append("(?<").append(name).append(">[^/]+)");
        }
        if (start != base.length()) {
            regexBuilder.append(base.substring(start));
        }
        // append ends
        regexBuilder.append("/*$");
        String regex = regexBuilder.toString();
        log.debug("Converted path pattern regex: {} >>> {}", pathPattern, regex);
        // check {.*}
        Matcher cm = anyPathVariablePattern.matcher(regex);
        if (cm.find()) {
            throw new IllegalArgumentException("illegal path variable " + cm.group());
        }
        Pattern pattern = Pattern.compile(regex);
        return pattern;
    }

    private static final class BasicPathPattern implements PathPattern {

        private final Pattern pattern;
        private final List<String> pathVariableNames;

        private BasicPathPattern(Pattern pattern, List<String> pathVariableNames) {
            this.pattern = pattern;
            this.pathVariableNames = pathVariableNames.isEmpty() ? emptyList() : unmodifiableList(pathVariableNames);
        }

        @Override
        public Pattern pattern() {
            return pattern;
        }

        @Override
        public List<String> pathVariableNames() {
            return pathVariableNames;
        }

    }

    private static final class ThreadLocalMatcherPathPattern extends ThreadLocal<Matcher> implements PathPattern {

        private final Pattern pattern;
        private final List<String> pathVariableNames;

        private ThreadLocalMatcherPathPattern(Pattern pattern, List<String> pathVariableNames) {
            this.pattern = pattern;
            this.pathVariableNames = pathVariableNames;

        }

        @Override
        protected Matcher initialValue() {
            return pattern.matcher("");
        }

        @Override
        public Pattern pattern() {
            return pattern;
        }

        @Override
        public List<String> pathVariableNames() {
            return pathVariableNames;
        }

        @Override
        public Matcher matcher(String path) {
            return get().reset(path);
        }

    }

    private PathPatternUtil() {
    }

}
