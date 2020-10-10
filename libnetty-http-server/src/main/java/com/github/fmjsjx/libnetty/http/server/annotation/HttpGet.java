package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.fmjsjx.libnetty.http.server.HttpMethodWrapper;

/**
 * Annotation for mapping HTTP {@code GET} requests onto specific handler
 * methods.
 *
 * <p>
 * Specifically, {@code @HttpGet} is a <em>composed annotation</em> that acts as
 * a shortcut for {@code @HttpRoute(method = HttpMethodWrapper.GET)}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Target(METHOD)
@Retention(RUNTIME)
@HttpRoute(method = HttpMethodWrapper.GET)
public @interface HttpGet {

    /**
     * Returns the path pattern of this route.
     * 
     * @return the path pattern of this route.
     */
    String[] value() default {};

}
