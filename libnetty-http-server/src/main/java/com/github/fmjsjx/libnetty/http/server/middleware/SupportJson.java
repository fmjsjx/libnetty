package com.github.fmjsjx.libnetty.http.server.middleware;

import java.io.Serial;
import java.lang.reflect.Type;
import java.util.concurrent.CompletionStage;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * A {@link Middleware} provides a {@link JsonLibrary} to support JSON features.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 * 
 * @see Middleware
 * @see MiddlewareChain
 * @see JsonLibrary
 */
@Deprecated(since = "1.3", forRemoval = true)
public class SupportJson implements Middleware {

    /**
     * Provides methods to support JSON features.
     * 
     * @author MJ Fang
     * 
     * @see Jackson2JsonLibrary
     * 
     * @deprecated please use
     *             {@link com.github.fmjsjx.libnetty.http.server.component.JsonLibrary}
     *             instead
     */
    @Deprecated(since = "1.3", forRemoval = true)
    public interface JsonLibrary {

        /**
         * The key of the {@link JsonLibrary} used in HTTP request context properties.
         * <p>
         * The value is
         * {@code com.github.fmjsjx.libnetty.http.server.middleware.SupportJson.JsonLibrary.class}.
         */
        Class<JsonLibrary> KEY = JsonLibrary.class;

        /**
         * Read a JSON value from the given content.
         * 
         * @param <T>       the type of the value to be converted to
         * @param content   a {@link ByteBuf}
         * @param valueType the common type of the value
         * @return the converted object
         */
        <T> T read(ByteBuf content, Type valueType);

        /**
         * Write a JSON value into the a {@link ByteBuf}.
         * 
         * @param alloc the {@link ByteBufAllocator} allocates {@link ByteBuf}s
         * @param value the value object
         * @return a {@code ByteBuf}
         */
        ByteBuf write(ByteBufAllocator alloc, Object value);

    }

    /**
     * A runtime exception threw by a JSON encoder/decoder.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     * 
     * @deprecated please use
     *             {@link com.github.fmjsjx.libnetty.http.server.component.JsonLibrary.JsonException}
     *             instead
     */
    @Deprecated
    public static class JsonException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 4697052174693197902L;

        /**
         * Constructs a new JSON exception with the specified detail message and cause.
         * 
         * @param message the detail message
         * @param cause   the cause
         */
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new JSON exception with the specified cause.
         * 
         * @param cause the cause
         */
        public JsonException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * Implementation of {@link JsonLibrary} using {@code jackson2}.
     * 
     * @author MJ Fang
     * 
     * @deprecated please use
     *             {@link com.github.fmjsjx.libnetty.http.server.component.Jackson2JsonLibrary}
     *             instead
     */
    @Deprecated(since = "1.3", forRemoval = true)
    public static class Jackson2JsonLibrary implements JsonLibrary {

        private final com.github.fmjsjx.libnetty.http.server.component.Jackson2JsonLibrary delegatedLibrary;

        /**
         * Constructs a new {@link Jackson2JsonLibrary} with the specified
         * {@link com.fasterxml.jackson.databind.ObjectMapper}.
         * 
         * @param objectMapper an {@code ObjectMapper}
         */
        public Jackson2JsonLibrary(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            this.delegatedLibrary = new com.github.fmjsjx.libnetty.http.server.component.Jackson2JsonLibrary(objectMapper);
        }

        /**
         * Constructs a new {@link Jackson2JsonLibrary} with the default
         * {@link com.fasterxml.jackson.databind.ObjectMapper}.
         */
        public Jackson2JsonLibrary() {
            this(com.github.fmjsjx.libnetty.http.server.component.Jackson2JsonLibrary.defaultObjectMapper());
        }

        @Override
        public <T> T read(ByteBuf content, Type valueType) {
            return delegatedLibrary.read(content, valueType);
        }

        @Override
        public ByteBuf write(ByteBufAllocator alloc, Object value) {
            return delegatedLibrary.write(alloc, value);
        }

    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        return next.doNext(ctx);
    }

}
