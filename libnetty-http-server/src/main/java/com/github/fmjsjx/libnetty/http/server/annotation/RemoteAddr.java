package com.github.fmjsjx.libnetty.http.server.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates method parameter should be bound to the remote
 * address (client IP).
 * <p>
 * Supported for {@link HttpRoute} annotated handler methods.
 * <p>
 * This value will be auto mixed with header "x-forwarded-for" (if present).
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface RemoteAddr {

}
