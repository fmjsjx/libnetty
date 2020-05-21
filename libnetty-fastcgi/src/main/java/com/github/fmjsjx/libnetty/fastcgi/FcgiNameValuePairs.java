package com.github.fmjsjx.libnetty.fastcgi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiConsumer;

/**
 * The abstract implementation of {@code FastCGI Name-Value Pairs}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class FcgiNameValuePairs<Self extends FcgiNameValuePairs<?>> extends AbstractFcgiRecord {

    protected final Map<String, String> pairs;

    protected FcgiNameValuePairs(FcgiVersion protocolVersion, int requestId) {
        this(protocolVersion, requestId, new LinkedHashMap<String, String>());
    }

    protected FcgiNameValuePairs(FcgiVersion protocolVersion, int requestId, Map<String, String> pairs) {
        super(protocolVersion, requestId);
        this.pairs = pairs;
    }

    /**
     * Performs the given action for each {@code Name-Value Pair} in this
     * {@link FcgiNameValuePairs}.
     * 
     * @param action the action to be performed
     */
    public void forEach(BiConsumer<String, String> action) {
        pairs.forEach(action);
    }

    /**
     * Put a {@code Name-Value Pair}.
     * 
     * @param name  the name of the pair
     * @param value the value of the pair
     * @return this instance
     */
    @SuppressWarnings("unchecked")
    public Self put(String name, Object value) {
        Objects.requireNonNull(value, "value must not be null");
        pairs.put(name, value.toString());
        return (Self) this;
    }

    /**
     * Removes the {@code Name-Value Pair} for the specified name from this
     * {@link FcgiNameValuePairs} if present.
     * 
     * @param name the name of the pair
     * @return the previous value of the pair with {@code name}, or {@code null} if
     *         there was pair for {@code name}.
     */
    public String remove(String name) {
        return pairs.remove(name);
    }

    /**
     * Returns the value of the {@code Name-Value Pair} with the specified
     * {@code name}.
     * 
     * @param name the name of the pair
     * @return an {@code Optional<String>}
     */
    public Optional<String> get(String name) {
        return Optional.ofNullable(pairs.get(name));
    }

    /**
     * Returns the {@code int} value of the {@code Name-Value Pair} with the
     * specified {@code name}.
     * 
     * @param name the name of the pair
     * @return an {@code OptionalInt}
     * @throws NumberFormatException if the value can't be parsed to {@code int}
     */
    public OptionalInt getInt(String name) throws NumberFormatException {
        String value = pairs.get(name);
        if (value == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(value));
    }

    /**
     * Returns the {@code long} value of the {@code Name-Value Pair} with the
     * specified {@code name}.
     * 
     * @param name the name of the pair
     * @return an {@code OptionalLong}
     * @throws NumberFormatException if the value can't be parsed to {@code long}
     */
    public OptionalLong getLong(String name) throws NumberFormatException {
        String value = pairs.get(name);
        if (value == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(value));
    }

    /**
     * Returns the names in this {@link FcgiNameValuePairs}.
     * 
     * @return an {@code Iterable<String>}
     */
    public Iterable<String> names() {
        return pairs.keySet();
    }

}
