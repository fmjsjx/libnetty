package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a parameter variable should be bound to an HTTP
 * path variable.
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
     * 
     * @return the name of the path variable
     */
    String value() default "";

}
