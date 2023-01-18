package com.github.fmjsjx.libnetty.http.server.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation that indicates method parameter should be bound to some cookie value of the HTTP request.
 *
 * @since 2.7
 *
 * @author MJ Fang
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface CookieValue {

    /**
     * Returns the name of the value in HTTP cookies.
     *
     * @return the name of the value in HTTP cookies
     */
    String value();

    /**
     * Returns whether the cookies is required.
     * <p>
     * The default is {@code true}.
     *
     * @return whether the cookies is required
     */
    boolean required() default true;

}
