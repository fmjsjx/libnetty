package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.fmjsjx.libnetty.http.server.component.HttpServerComponent;

/**
 * Annotation that indicates method parameter should be bound to some component
 * value of the HTTP server.
 * 
 * @since 1.3
 *
 * @author MJ Fang
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface ComponentValue {

    /**
     * Returns the type of the component.
     * <p>
     * Default value is {@code HttpServerComponent.class}.
     * 
     * @return the type of the component.
     */
    Class<? extends HttpServerComponent> value() default HttpServerComponent.class;

    /**
     * Returns whether the component value is required.
     * <p>
     * The default is {@code true}.
     * 
     * @return {@code true} if the component value is required, {@code false}
     *         otherwise
     */
    boolean required() default true;

}
