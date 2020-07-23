package com.github.fmjsjx.libnetty.http.server;

import java.util.function.Supplier;

/**
 * Provides {@link HttpServerHandler}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface HttpServerHandlerProvider extends Supplier<HttpServerHandler> {

}
