package com.github.fmjsjx.libnetty.http.server.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

/**
 * Implementation of {@link JsonLibrary} using {@code jackson2}.
 * 
 * @author MJ Fang
 * 
 * @since 1.3
 */
public class Jackson2JsonLibrary implements JsonLibrary {

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
                if (com.fasterxml.jackson.databind.JsonNode.class.isAssignableFrom((Class<T>) valueType)) {
                    return (T) objectMapper.readTree(src);
                }
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
            buf.release();
            throw new JsonException(e.getMessage(), e);
        }
    }

    
}
