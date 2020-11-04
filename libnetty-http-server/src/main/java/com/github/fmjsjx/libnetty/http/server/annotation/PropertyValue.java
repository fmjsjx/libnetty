package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates method parameter should be bound to some property
 * value of the request context.
 * 
 * @since 1.2
 *
 * @author MJ Fang
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PropertyValue {

    /**
     * Returns the property key string.
     * 
     * @return the property key string
     */
    String value() default "";

    /**
     * Returns whether the property value is required.
     * <p>
     * The default is {@code true}.
     * 
     * @return whether the property value is required
     */
    boolean required() default true;

}
