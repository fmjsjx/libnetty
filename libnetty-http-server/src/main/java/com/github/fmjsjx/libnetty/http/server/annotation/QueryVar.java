package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a parameter variable should be bound to an HTTP
 * query parameter.
 * <p>
 * Supported for {@link HttpRoute} annotated handler methods
 * 
 * @since 1.1
 *
 * @author MJ Fang
 * 
 * @see HttpRoute
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface QueryVar {

    /**
     * Returns the name of the path variable.
     * 
     * @return the name of the path variable
     */
    String value() default "";

    /**
     * Returns whether the path variable is required.
     * <p>
     * The default is {@code true}.
     * 
     * @return whether the path variable is required
     */
    boolean required() default true;

    /**
     * Returns whether the query name is compatible with array style.
     * <p>
     * If {@code true} then the query name "name" or "name[]" will
     * be treated as the same parameter.
     * <p>
     * The default is {@code true}.
     *
     * @return whether the query name is compatible with array style.
     * @since 3.4
     */
    boolean compatibleWithArray() default true;

}
