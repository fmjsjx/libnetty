package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The abstract implementation of {@code FastCGI Name-Value Pairs}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class FcgiNameValuePairs<Self extends FcgiNameValuePairs<?>> extends AbstractFcgiRecord {

    protected final LinkedHashMap<String, NameValuePair> pairs = new LinkedHashMap<>();

    protected FcgiNameValuePairs(FcgiVersion protocolVersion, int requestId) {
        super(protocolVersion, requestId);
    }

    /**
     * Performs the given action for each {@code Name-Value Pair} in this
     * {@link FcgiNameValuePairs}.
     * 
     * @param action the action to be performed
     */
    public void forEach(Consumer<NameValuePair> action) {
        pairs.values().forEach(action);
    }

    /**
     * Performs the given action for each {@code Name-Value Pair} in this
     * {@link FcgiNameValuePairs}.
     * 
     * @param action the action to be performed
     */
    public void forEach(BiConsumer<String, String> action) {
        forEach(p -> action.accept(p.name, p.value));
    }

    /**
     * Put a {@code Name-Value Pair}.
     * 
     * @param name  the name of the pair
     * @param value the value of the pair
     * @return this instance
     */
    public Self put(String name, Object value) {
        return put(new NameValuePair(name, value));
    }

    /**
     * Put a {@code Name-Value Pair}.
     * 
     * @param pair the pair
     * @return this instance
     */
    @SuppressWarnings("unchecked")
    public Self put(NameValuePair pair) {
        pairs.put(pair.name(), pair);
        return (Self) this;
    }

    /**
     * Removes the {@code Name-Value Pair} for the specified name from this
     * {@link FcgiNameValuePairs} if present.
     * 
     * @param name the name of the pair
     * @return an {@code Optional<NameValuePair>}
     */
    public Optional<NameValuePair> remove(String name) {
        return Optional.ofNullable(pairs.remove(name));
    }

    /**
     * Returns the {@code Name-Value Pair} with the specified {@code name}.
     * 
     * @param name the name of the pair
     * @return an {@code Optional<NameValuePair>}
     */
    public Optional<NameValuePair> get(String name) {
        return Optional.ofNullable(pairs.get(name));
    }

    /**
     * Returns the value of the {@code Name-Value Pair} with the specified
     * {@code name}.
     * 
     * @param name the name of the pair
     * @return an {@code Optional<String>}
     */
    public Optional<String> getValue(String name) {
        return get(name).map(NameValuePair::value);
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
        NameValuePair pair = pairs.get(name);
        if (pair == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(pair.intValue());
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
        NameValuePair pair = pairs.get(name);
        if (pair == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(pair.longValue());
    }

    /**
     * Returns the names in this {@link FcgiNameValuePairs}.
     * 
     * @return an {@code Iterable<String>}
     */
    public Iterable<String> names() {
        return pairs.keySet();
    }

    /**
     * Returns the number of pairs in this {@link FcgiNameValuePairs}.
     * 
     * @return the number of pairs
     */
    public int size() {
        return pairs.size();
    }

    /**
     * Returns an unmodifiable collection of pairs in this
     * {@link FcgiNameValuePairs}.
     * 
     * @return an {@code unmodifiable} collection
     */
    public Collection<NameValuePair> pairs() {
        return Collections.unmodifiableCollection(pairs.values());
    }

    /**
     * A Name-Value Pair.
     * 
     * @since 1.0
     *
     * @author MJ Fang
     */
    public static final class NameValuePair {

        private final String name;
        private final String value;

        protected NameValuePair(String name, Object value) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.value = Objects.requireNonNull(value, "value must not be null").toString();
        }

        protected NameValuePair(String name, String value) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.value = Objects.requireNonNull(value, "value must not be null");
        }

        /**
         * Returns the name of this pair.
         * 
         * @return the name of this pair
         */
        public String name() {
            return name;
        }

        /**
         * Returns the value of this pair.
         * 
         * @return the value of this pair
         */
        public String value() {
            return value;
        }

        /**
         * Returns the {@code int} value of this pair.
         * 
         * @return the value of this pair as {@code int} type
         * @throws NumberFormatException if the value can't be parsed to {@code int}
         */
        public int intValue() throws NumberFormatException {
            return Integer.parseInt(value);
        }

        /**
         * Returns the {@code long} value of this pair.
         * 
         * @return the value of this pair as {@code long} type
         * @throws NumberFormatException if the value can't be parsed to {@code long}
         */
        public long longValue() throws NumberFormatException {
            return Long.parseLong(value);
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NameValuePair) {
                NameValuePair other = (NameValuePair) obj;
                return name.equals(other.name) && value.equals(other.value);
            }
            return false;
        }

    }

    @Override
    protected void bodyToString(StringBuilder builder) {
        builder.append('{');
        boolean hasValue = false;
        for (NameValuePair pair : pairs()) {
            if (hasValue) {
                builder.append(", ");
            } else {
                hasValue = true;
            }
            builder.append("(\"").append(pair.name).append("\", \"").append(pair.value).append("\")");
        }
        builder.append('}');
    }

}
