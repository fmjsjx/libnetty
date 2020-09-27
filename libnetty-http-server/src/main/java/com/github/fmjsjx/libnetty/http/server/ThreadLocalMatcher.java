package com.github.fmjsjx.libnetty.http.server;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a thread-local variables of {@link Matcher}s.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class ThreadLocalMatcher extends ThreadLocal<Matcher> {

    protected final Pattern pattern;

    /**
     * Construct a new {@link ThreadLocalMatcher} instance with the specified
     * pattern.
     * 
     * @param pattern the pattern that create matchers
     */
    public ThreadLocalMatcher(Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
    }

    /**
     * Return the pattern that create matchers.
     * 
     * @return the pattern that create matchers
     */
    public Pattern pattern() {
        return pattern;
    }

    @Override
    protected Matcher initialValue() {
        return pattern.matcher("");
    }

    /**
     * Returns the thread-local matcher.
     * <p>
     * This method is equivalent to {@link #get()}.
     * 
     * @return the thread-local matcher
     */
    public Matcher matcher() {
        return get();
    }

    /**
     * Resets the thread-local matcher with a new input sequence.
     * 
     * @param input the new input character sequence
     * @return the thread-local matcher
     */
    public Matcher reset(CharSequence input) {
        return matcher().reset(input);
    }

}
