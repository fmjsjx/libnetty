package com.github.fmjsjx.libnetty.http.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Provides methods to access path variables from an HTTP path.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface PathVariables {

    /**
     * The empty {@link PathVariables} instances.
     */
    PathVariables EMPTY = EmptyPathVariables.INSTANCE;

    /**
     * Returns the path variable by the name given.
     * 
     * @param name the name of the path variable
     * @return {@code Optional<String>}
     */
    Optional<String> getString(String name);

    /**
     * Returns the path variable as {@code int} type by the name given.
     * 
     * @param name the name of the path variable
     * @return {@code OptionalInt}
     */
    OptionalInt getInt(String name);

    /**
     * Returns the path variable as {@code long} type by the name given.
     * 
     * @param name the name of the path variable
     * @return {@code OptionalLong}
     */
    OptionalLong getLong(String name);

    /**
     * Returns the path variable as {@code double} type by the name given.
     * 
     * @param name the name of the path variable
     * @return {@code OptionalDouble}
     */
    OptionalDouble getDouble(String name);

    /**
     * Returns if the path variable by the name given exists.
     * 
     * @param name the name of the path variable
     * @return {@code true} if the path variable by the name given exists,
     *         {@code false} otherwise
     */
    boolean exists(String name);

    /**
     * Returns the size of the path variables.
     * 
     * @return the size of the path variables
     */
    int size();

    /**
     * Returns a collection contains of the names of all path variables.
     * 
     * @return a collection contains of the names of all path variables
     */
    Collection<String> names();

}

class EmptyPathVariables implements PathVariables {

    static final EmptyPathVariables INSTANCE = new EmptyPathVariables();

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Collection<String> names() {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getString(String name) {
        return Optional.empty();
    }

    @Override
    public OptionalLong getLong(String name) {
        // TODO Auto-generated method stub
        return OptionalLong.empty();
    }

    @Override
    public OptionalInt getInt(String name) {
        // TODO Auto-generated method stub
        return OptionalInt.empty();
    }

    @Override
    public OptionalDouble getDouble(String name) {
        return OptionalDouble.empty();
    }

    @Override
    public boolean exists(String name) {
        return false;
    }

    @Override
    public String toString() {
        return "PathVariables{}";
    }

}