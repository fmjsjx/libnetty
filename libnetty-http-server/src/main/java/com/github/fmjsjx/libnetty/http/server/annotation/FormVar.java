package com.github.fmjsjx.libnetty.http.server.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation that indicates a method parameter should be bound to an
 * HTTP form variable.
 * <p>
 * Supported for {@link HttpRoute} annotated handler methods
 *
 * @author MJ Fang
 * @see HttpRoute
 * @since 4.3
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface FormVar {

    /**
     * Returns the name of the HTTP form variable.
     *
     * @return the name of the HTTP form variable
     */
    String value() default "";

    /**
     * Returns whether the form variable is required.
     * <p>
     * The default is {@code true}.
     *
     * @return whether the form variable is required
     */
    boolean required() default true;

}
