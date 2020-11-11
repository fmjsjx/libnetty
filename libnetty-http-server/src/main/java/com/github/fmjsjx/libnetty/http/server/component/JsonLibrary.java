package com.github.fmjsjx.libnetty.http.server.component;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * Provides methods to support JSON features.
 * 
 * @author MJ Fang
 * 
 * @see Jackson2JsonLibrary
 * 
 * @since 1.3
 */
public interface JsonLibrary extends HttpServerComponent {

    /**
     * Returns the instance which implements the {@link JsonLibrary}.
     * 
     * @return the instance which implements the {@link JsonLibrary}
     */
    static JsonLibrary getInstance() {
        return JsonLibraries.library;
    }

    @Override
    default Class<JsonLibrary> componentType() {
        return JsonLibrary.class;
    }

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

    /**
     * A runtime exception threw by a JSON encoder/decoder.
     * 
     * @since 1.3
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

}

final class JsonLibraries {

    private static final Logger logger = LoggerFactory.getLogger(JsonLibraries.class);

    static final JsonLibrary library;

    static {
        library = lookupLibrary();
    }

    static final JsonLibrary lookupLibrary() {
        String propertyKey = "libnetty.http.server.component.json.library";
        String libraryClassName = SystemPropertyUtil.get(propertyKey);
        logger.debug("-D{}: {}", propertyKey, libraryClassName);
        if (libraryClassName != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends JsonLibrary> libraryClass = (Class<? extends JsonLibrary>) Class
                        .forName(libraryClassName);
                return libraryClass.getConstructor().newInstance();
            } catch (Exception e) {
                logger.warn("Create specified JSON library {} failed, use default library (jackson2) instead.",
                        libraryClassName, e);
            }
        }
        logger.debug("Lookup jackson2 in classpath.");
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return new Jackson2JsonLibrary();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Can't find any available JsonLibrary in class path.", e);
        }
    }

}