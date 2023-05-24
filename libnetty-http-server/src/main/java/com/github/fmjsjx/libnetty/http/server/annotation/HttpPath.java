package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for path of HTTP services.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(HttpPaths.class)
public @interface HttpPath {

    /**
     * Returns the value string of the path.
     * 
     * @return the value string of the path
     */
    String[] value() default {};

}
