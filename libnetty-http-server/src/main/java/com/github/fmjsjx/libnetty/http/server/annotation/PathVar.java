package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for HTTP path variable.
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
public @interface PathVar {

    /**
     * Returns the name of the path variable.
     * <p>
     * 
     * 
     * @return the name of the path variable
     */
    String name();

    /**
     * Returns whether the path variable is required.
     * <p>
     * The default is {@code true}.
     * 
     * @return whether the path variable is required
     */
    boolean required() default true;

}
