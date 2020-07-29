package com.github.fmjsjx.libnetty.http.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathPatternUtil {

    private static final Logger log = LoggerFactory.getLogger(PathPatternUtil.class);

    private static final Pattern pathVariablePattern = Pattern.compile("\\{[\\w\\-]+\\}");
    private static final Pattern anyPathVariablePattern = Pattern.compile("\\{.+\\}");

    /**
     * Build a new {@link Pattern} from HTTP path pattern.
     * 
     * @param pathPattern the pattern of HTTP path
     * @return a {@code Pattern}
     * 
     * @throws IllegalArgumentException if the path variable is illegal
     */
    public static final Pattern fromPathPattern(String pathPattern) throws IllegalArgumentException {
        String base = pathPattern.replace("-", "\\-").replace(".", "\\.");
        Matcher m = pathVariablePattern.matcher(base);
        StringBuilder b = new StringBuilder().append("^");
        int start = 0;
        for (; m.find(start); start = m.end()) {
            b.append(base.substring(start, m.start()));
            String g = m.group();
            String name = g.substring(1, g.length() - 1);
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
        return Pattern.compile(converted);
    }

    private PathPatternUtil() {
    }

}
