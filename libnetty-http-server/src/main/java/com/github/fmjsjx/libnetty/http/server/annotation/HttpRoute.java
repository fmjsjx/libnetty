package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.fmjsjx.libnetty.http.server.HttpMethodWrapper;

/**
 * Annotation for HTTP route.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Target({ METHOD, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface HttpRoute {

    /**
     * Returns the path pattern of this route.
     * 
     * @return the path pattern of this route.
     */
    String[] value() default {};

    /**
     * Returns the methods of this route.
     * 
     * @return the methods of this route
     */
    HttpMethodWrapper[] method() default {};

}
