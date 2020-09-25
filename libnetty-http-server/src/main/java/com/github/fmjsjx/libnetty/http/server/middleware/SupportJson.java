package com.github.fmjsjx.libnetty.http.server.middleware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

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
public class SupportJson implements Middleware {

    /**
     * Provides methods to support JSON features.
     * 
     * @author MJ Fang
     */
    public interface JsonLibrary {

        /**
         * The key of the {@link JsonLibrary} used in HTTP request context properties.
         * <p>
         * The value is
         * {@code com.github.fmjsjx.libnetty.http.server.middleware.SupportJson.JsonLibrary.class}.
         */
        Object KEY = JsonLibrary.class;

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
     */
    public static class JsonException extends RuntimeException {

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
     */
    public static class Jackson2JsonLibrary implements JsonLibrary {

        private static final ConcurrentMap<Type, com.fasterxml.jackson.databind.JavaType> cachedJavaTypes = new ConcurrentHashMap<>();

        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        /**
         * Constructs a new {@link Jackson2JsonLibrary} with the specified
         * {@link com.fasterxml.jackson.databind.ObjectMapper}.
         * 
         * @param objectMapper an {@code ObjectMapper}
         */
        public Jackson2JsonLibrary(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        }

        /**
         * Constructs a new {@link Jackson2JsonLibrary} with the default
         * {@link com.fasterxml.jackson.databind.ObjectMapper}.
         */
        public Jackson2JsonLibrary() {
            this(new com.fasterxml.jackson.databind.ObjectMapper()
                    .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT)
                    .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T read(ByteBuf content, Type valueType) {
            try (InputStream src = new ByteBufInputStream(content.duplicate())) {
                if (valueType instanceof Class) {
                    return objectMapper.readValue(src, (Class<T>) valueType);
                }
                com.fasterxml.jackson.databind.JavaType javaType = cachedJavaTypes.computeIfAbsent(valueType,
                        objectMapper::constructType);
                return objectMapper.readValue(src, javaType);
            } catch (IOException e) {
                throw new JsonException(e.getMessage(), e);
            }
        }

        @Override
        public ByteBuf write(ByteBufAllocator alloc, Object value) {
            ByteBuf buf = alloc.buffer();
            try (OutputStream out = new ByteBufOutputStream(buf)) {
                objectMapper.writeValue(out, value);
                return buf;
            } catch (IOException e) {
                throw new JsonException(e.getMessage(), e);
            }
        }

    }

    private final JsonLibrary library;

    /**
     * Constructs a new {@link SupportJson} instance with the specified JSON
     * library.
     * 
     * @param library a {@link JsonLibrary}
     */
    public SupportJson(JsonLibrary library) {
        this.library = Objects.requireNonNull(library, "library must not be null");
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        ctx.property(JsonLibrary.KEY, library);
        return next.doNext(ctx);
    }

}
