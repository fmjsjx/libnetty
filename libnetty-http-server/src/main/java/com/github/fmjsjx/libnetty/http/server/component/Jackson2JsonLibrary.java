package com.github.fmjsjx.libnetty.http.server.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final Logger logger = LoggerFactory.getLogger(Jackson2JsonLibrary.class);

    private static final Optional<com.fasterxml.jackson.datatype.jdk8.Jdk8Module> jdk8Module;
    private static final Optional<com.fasterxml.jackson.datatype.jsr310.JavaTimeModule> javaTimeModule;

    static {
        Optional<com.fasterxml.jackson.datatype.jdk8.Jdk8Module> _jdk8Module;
        try {
            Class.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module");
            _jdk8Module = Optional.of(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
        } catch (ClassNotFoundException e) {
            logger.debug("Class<com.fasterxml.jackson.datatype.jdk8.Jdk8Module> not found, jdk8Module disabled");
            _jdk8Module = Optional.empty();
        }
        jdk8Module = _jdk8Module;
        Optional<com.fasterxml.jackson.datatype.jsr310.JavaTimeModule> _javaTimeModule;
        try {
            Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            _javaTimeModule = Optional.of(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        } catch (ClassNotFoundException e) {
            logger.debug(
                    "Class<com.fasterxml.jackson.datatype.jsr310.JavaTimeModule> not found, javaTimeModule disabled");
            _javaTimeModule = Optional.empty();
        }
        javaTimeModule = _javaTimeModule;
    }

    public static final ObjectMapper defaultObjectMapper() {
        ObjectMapper om = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jdk8Module.ifPresent(om::registerModule);
        javaTimeModule.ifPresent(om::registerModule);
        return om;
    }

    public static final boolean jdk8ModuleEnabled() {
        return jdk8Module.isPresent();
    }

    public static final com.fasterxml.jackson.datatype.jdk8.Jdk8Module jdk8Module() {
        return jdk8Module.get();
    }

    public static final boolean javaTimeModuleEnabled() {
        return javaTimeModule.isPresent();
    }

    public static final com.fasterxml.jackson.datatype.jsr310.JavaTimeModule javaTimeModule() {
        return javaTimeModule.get();
    }

    private static final ConcurrentMap<Type, JavaType> cachedJavaTypes = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@link Jackson2JsonLibrary} with the specified
     * {@link com.fasterxml.jackson.databind.ObjectMapper}.
     * 
     * @param objectMapper an {@code ObjectMapper}
     */
    public Jackson2JsonLibrary(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Constructs a new {@link Jackson2JsonLibrary} with the default
     * {@link com.fasterxml.jackson.databind.ObjectMapper}.
     */
    public Jackson2JsonLibrary() {
        this(defaultObjectMapper());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(ByteBuf content, Type valueType) {
        try (InputStream src = new ByteBufInputStream(content.duplicate())) {
            if (valueType instanceof Class) {
                if (JsonNode.class.isAssignableFrom((Class<T>) valueType)) {
                    return (T) objectMapper.readTree(src);
                }
                return objectMapper.readValue(src, (Class<T>) valueType);
            }
            JavaType javaType = cachedJavaTypes.computeIfAbsent(valueType, objectMapper::constructType);
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
