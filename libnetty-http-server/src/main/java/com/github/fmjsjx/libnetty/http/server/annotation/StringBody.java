package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a method return value or a method parameter should
 * be bound to the HTTP body as type {@code "text/plain"}.
 * <p>
 * Supported for {@link HttpRoute} annotated handler methods
 * 
 * @since 2.0
 *
 * @author MJ Fang
 * 
 * @see HttpRoute
 */
@Target({ METHOD, PARAMETER })
@Retention(RUNTIME)
public @interface StringBody {

}
