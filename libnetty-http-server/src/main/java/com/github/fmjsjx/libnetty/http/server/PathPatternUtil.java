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
     * @param pathPattern the pattern of HTTP path
     * 
     * @return a {@code PathPattern}
     * 
     * @throws IllegalArgumentException if the path variable is illegal
     */
    public static final PathPattern build(String pathPattern) throws IllegalArgumentException {
        String base = pathPattern.replace("-", "\\-").replace(".", "\\.");
        Matcher m = pathVariablePattern.matcher(base);
        ArrayList<String> pathVariableNames = new ArrayList<String>();
        StringBuilder b = new StringBuilder().append("^");
        int start = 0;
        for (; m.find(start); start = m.end()) {
            b.append(base.substring(start, m.start()));
            String g = m.group();
            String name = g.substring(1, g.length() - 1);
            pathVariableNames.add(name);
            b.append("(?<").append(name).append(">[^/]+)");
        }
        if (start != base.length()) {
            b.append(base.substring(start));
        }
        if (base.endsWith("/")) {
            b.append("?");
        } else {
            b.append("/?");
        }
        String converted = b.toString();
        log.debug("Converted path pattern: {} >>> {}", pathPattern, converted);
        // check {.*}
        Matcher cm = anyPathVariablePattern.matcher(converted);
        if (cm.find()) {
            throw new IllegalArgumentException("illegal path variable " + cm.group());
        }
        Pattern pattern = Pattern.compile(converted);
        return new PathPatternImpl(pattern, pathVariableNames);
    }

    private static final class PathPatternImpl implements PathPattern {

        private final Pattern pattern;
        private final List<String> pathVariableNames;

        private PathPatternImpl(Pattern pattern, List<String> pathVariableNames) {
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

    private PathPatternUtil() {
    }

}
