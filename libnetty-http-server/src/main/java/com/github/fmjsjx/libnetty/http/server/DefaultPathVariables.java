package com.github.fmjsjx.libnetty.http.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * The default implementation of {@link PathVariables}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultPathVariables implements PathVariables {

    private static final class PathVariable {

        private final String value;
        private final Optional<String> stringValue;
        private volatile OptionalInt intValue;
        private volatile OptionalLong longValue;
        private volatile OptionalDouble doubleValue;

        private PathVariable(String value) {
            this.value = value;
            this.stringValue = Optional.of(value);
        }

        private Optional<String> stringValue() {
            return stringValue;
        }

        private OptionalInt intValue() {
            OptionalInt intValue = this.intValue;
            if (intValue == null) {
                this.intValue = intValue = OptionalInt.of(Integer.parseInt(value));
            }
            return intValue;
        }

        private OptionalLong longValue() {
            OptionalLong longValue = this.longValue;
            if (longValue == null) {
                this.longValue = longValue = OptionalLong.of(Long.parseLong(value));
            }
            return longValue;
        }

        private OptionalDouble doubleValue() {
            OptionalDouble doubleValue = this.doubleValue;
            if (doubleValue == null) {
                this.doubleValue = doubleValue = OptionalDouble.of(Double.parseDouble(value));
            }
            return doubleValue;
        }

    }

    private final Map<String, PathVariable> variables;

    /**
     * Constructs a new {@link DefaultPathVariables}.
     */
    public DefaultPathVariables() {
        this.variables = new LinkedHashMap<>();
    }

    /**
     * Constructs a new {@link DefaultPathVariables} with the given map.
     * 
     * @param map a key-value map constants the path variables
     */
    public DefaultPathVariables(Map<String, String> map) {
        this();
        map.forEach(this::put);
    }

    /**
     * Puts a path variable by the specified name and value.
     * 
     * @param name  the name of the path variable
     * @param value the value string of the path variable
     */
    public void put(String name, String value) {
        variables.put(name, new PathVariable(value));
    }

    private PathVariable get(String name) {
        return variables.get(name);
    }

    @Override
    public Optional<String> getString(String name) {
        PathVariable variable = get(name);
        if (variable == null) {
            return Optional.empty();
        }
        return variable.stringValue();
    }

    @Override
    public OptionalInt getInt(String name) {
        PathVariable variable = get(name);
        if (variable == null) {
            return OptionalInt.empty();
        }
        return variable.intValue();
    }

    @Override
    public OptionalLong getLong(String name) {
        PathVariable variable = get(name);
        if (variable == null) {
            return OptionalLong.empty();
        }
        return variable.longValue();
    }

    @Override
    public OptionalDouble getDouble(String name) {
        PathVariable variable = get(name);
        if (variable == null) {
            return OptionalDouble.empty();
        }
        return variable.doubleValue();
    }

    @Override
    public boolean exists(String name) {
        return variables.containsKey(name);
    }

    @Override
    public int size() {
        return variables.size();
    }

    @Override
    public Collection<String> names() {
        return variables.keySet();
    }

}
