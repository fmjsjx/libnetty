package com.github.fmjsjx.libnetty.http.server.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for paths of HTTP services.
 *
 * @author MJ Fang
 * @since 3.1
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface HttpPaths {

    /**
     * Returns the {@link HttpPath}s.
     *
     * @return the {@code HttpPath}s
     */
    HttpPath[] value();

}
