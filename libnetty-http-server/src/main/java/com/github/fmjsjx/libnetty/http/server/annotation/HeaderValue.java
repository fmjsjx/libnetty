package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates method parameter should be bound to the HTTP header
 * with the specified name.
 * <p>
 * Supported for {@link HttpRoute} annotated handler methods
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface HeaderValue {

    /**
     * Returns the name of the value in HTTP header.
     * 
     * @return the name of the value in HTTP header
     */
    String value();

}
