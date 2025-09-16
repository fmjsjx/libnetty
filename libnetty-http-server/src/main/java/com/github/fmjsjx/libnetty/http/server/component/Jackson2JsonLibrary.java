package com.github.fmjsjx.libnetty.http.server.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.fmjsjx.libcommon.util.KotlinUtil;
import com.github.fmjsjx.libcommon.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Jackson2JsonLibrary extends AbstractJsonLibrary {

    private static final Logger logger = LoggerFactory.getLogger(Jackson2JsonLibrary.class);

    private static final Optional<Module> jdk8Module;
    private static final Optional<Module> javaTimeModule;
    private static final Optional<Module> kotlinModule;

    static {
        jdk8Module = ReflectUtil.constructForClassName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module");
        if (jdk8Module.isEmpty()) {
            logger.debug("Class<com.fasterxml.jackson.datatype.jdk8.Jdk8Module> not found, jdk8Module disabled");
        }
        javaTimeModule = ReflectUtil.constructForClassName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
        if (javaTimeModule.isEmpty()) {
            logger.debug(
                    "Class<com.fasterxml.jackson.datatype.jsr310.JavaTimeModule> not found, javaTimeModule disabled");
        }
        if (KotlinUtil.isKotlinPresent()) {
            kotlinModule = ReflectUtil.constructForClassName("com.fasterxml.jackson.module.kotlin.KotlinModule");
        } else {
            kotlinModule = Optional.empty();
        }
    }

    /**
     * Creates and returns a new {@link ObjectMapper} with <b>default</b> options.
     *
     * <p>"<b>default</b>" means:</p>
     * <ul>
     *     <li>{@code NON_ABSENT}</li>
     *     <li>disable {@code FAIL_ON_UNKNOWN_PROPERTIES}</li>
     *     <li>{@code Jdk8Module} if available</li>
     *     <li>{@code JavaTimeModule} if available</li>
     *     <li>{@code KotlinModule} if kotlin present and module available</li>
     * </ul>
     *
     * @return the created {@code ObjectMapper} instance
     */
    public static final ObjectMapper defaultObjectMapper() {
        ObjectMapper om = new ObjectMapper().setDefaultPropertyInclusion(Include.NON_ABSENT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jdk8Module.ifPresent(om::registerModule);
        javaTimeModule.ifPresent(om::registerModule);
        kotlinModule.ifPresent(om::registerModule);
        return om;
    }

    /**
     * Returns whether Jdk8Module is enabled or not.
     *
     * @return {@code true} if Jdk8Module is enabled, {@code false} otherwise
     */
    public static final boolean jdk8ModuleEnabled() {
        return jdk8Module.isPresent();
    }

    /**
     * Returns the global Jdk8Module instance.
     *
     * @return the global Jdk8Module instance if enabled, {@code null} otherwise
     */
    public static final Module jdk8Module() {
        return jdk8Module.orElse(null);
    }

    /**
     * Returns whether JavaTimeModule is enabled or not.
     *
     * @return {@code true} if JavaTimeModule is enabled, {@code false} otherwise
     */
    public static final boolean javaTimeModuleEnabled() {
        return javaTimeModule.isPresent();
    }

    /**
     * Returns the global JavaTimeModule instance.
     *
     * @return the global JavaTimeModule instance if enabled, {@code null} otherwise
     */
    public static final Module javaTimeModule() {
        return javaTimeModule.orElse(null);
    }

    /**
     * Returns whether KotlinModule is enabled or not.
     *
     * @return {@code true} if KotlinModule is enabled, {@code false} otherwise
     * @since 2.5
     */
    public static final boolean kotlinModuleEnabled() {
        return kotlinModule.isPresent();
    }

    /**
     * Returns the global KotlinModule instance.
     *
     * @return the global KotlinModule instance if enabled, {@code null} otherwise
     * @since 2.5
     */
    public static final Module kotlinModule() {
        return kotlinModule.orElse(null);
    }

    private static final ConcurrentMap<Type, JavaType> cachedJavaTypes = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@link Jackson2JsonLibrary} with the specified
     * {@link ObjectMapper} and the default {@link EmptyWay} {@code NULL}.
     * 
     * @param objectMapper an {@code ObjectMapper}
     */
    public Jackson2JsonLibrary(ObjectMapper objectMapper) {
        this(objectMapper, EmptyWay.NULL);
    }

    /**
     * Constructs a new {@link Jackson2JsonLibrary} with the default
     * {@link ObjectMapper} and the default  {@link EmptyWay} {@code NULL}.
     */
    public Jackson2JsonLibrary() {
        this(defaultObjectMapper(), EmptyWay.NULL);
    }

    /**
     * Constructs a new {@link Jackson2JsonLibrary} with the specified
     * {@link ObjectMapper} and the specified {@link EmptyWay} given.
     *
     * @param objectMapper an {@code ObjectMapper}
     * @param emptyWay     an {@code EmptyWay}
     * @author MJ Fang
     * @since 3.6
     */
    public Jackson2JsonLibrary(ObjectMapper objectMapper, EmptyWay emptyWay) {
        super(emptyWay);
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Constructs a new {@link Jackson2JsonLibrary} with the default
     * {@link ObjectMapper} and the specified {@link EmptyWay} given.
     *
     * @param emptyWay an {@code EmptyWay}
     * @author MJ Fang
     * @since 3.6
     */
    public Jackson2JsonLibrary(EmptyWay emptyWay) {
        this(defaultObjectMapper(), emptyWay);
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
            throw new JsonReadException(e.getMessage(), e);
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
            throw new JsonWriteException(e.getMessage(), e);
        }
    }

}
