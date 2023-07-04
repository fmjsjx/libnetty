package com.github.fmjsjx.libnetty.http.server.component;

import java.io.Serial;
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

        @Serial
        private static final long serialVersionUID = 4697052174693197902L;

        /**
         * Constructs a new JSON exception with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause   the cause
         * @deprecated please use {@link JsonReadException} or {@link JsonWriteException} instead
         */
        @Deprecated
        public JsonException(String message, Throwable cause) {
            this(message, cause, null);
        }

        /*
         * Package private constructor.  Trailing Void argument is there for
         * disambiguating it against other (public) constructors.
         */
        JsonException(String message, Throwable cause, @SuppressWarnings("unused") Void sig) {
            super(message, cause);
        }

        /**
         * Constructs a new JSON exception with the specified cause.
         *
         * @param cause the cause
         * @deprecated please use {@link JsonReadException} or {@link JsonWriteException} instead
         */
        @Deprecated
        public JsonException(Throwable cause) {
            this(cause, null);
        }

        /*
         * Package private constructor.  Trailing Void argument is there for
         * disambiguating it against other (public) constructors.
         */
        JsonException(Throwable cause, @SuppressWarnings("unused") Void sig) {
            super(cause);
        }

    }

    /**
     * A runtime exception threw by a JSON reader.
     *
     * @author MJ Fang
     * @since 3.2
     */
    public static class JsonReadException extends JsonException {

        /**
         * Constructs a new {@link JsonReadException} with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause   the cause
         */
        public JsonReadException(String message, Throwable cause) {
            super(message, cause, null);
        }

        /**
         * Constructs a new {@link JsonReadException} with the specified cause.
         *
         * @param cause the cause
         */
        public JsonReadException(Throwable cause) {
            super(cause, null);
        }

    }

    /**
     * A runtime exception threw by a JSON writer.
     *
     * @author MJ Fang
     * @since 3.2
     */
    public static class JsonWriteException extends JsonException {

        /**
         * Constructs a new {@link JsonWriteException} with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause   the cause
         */
        public JsonWriteException(String message, Throwable cause) {
            super(message, cause, null);
        }

        /**
         * Constructs a new {@link JsonWriteException} with the specified cause.
         *
         * @param cause the cause
         */
        public JsonWriteException(Throwable cause) {
            super(cause, null);
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
            logger.debug("Lookup fastjson2 in classpath.");
            try {
                Class.forName("com.alibaba.fastjson2.JSON");
                return new Fastjson2JsonLibrary();
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("Can't find any available JsonLibrary in class path.", e);
            }
        }
    }

}