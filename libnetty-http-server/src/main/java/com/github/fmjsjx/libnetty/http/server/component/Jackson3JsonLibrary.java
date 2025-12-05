package com.github.fmjsjx.libnetty.http.server.component;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

import com.github.fmjsjx.libcommon.util.KotlinUtil;
import com.github.fmjsjx.libcommon.util.ReflectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of {@link JsonLibrary} using {@code Jackson3}.
 *
 * @author MJ Fang
 * @since 3.10
 */
public class Jackson3JsonLibrary extends AbstractJsonLibrary {

    private static final class KotlinModuleHolder {

        private static final JacksonModule kotlinModule = KotlinUtil.isKotlinPresent()
                ? ReflectUtil.<JacksonModule>constructForClassName("tools.jackson.module.kotlin.KotlinModule").orElse(null)
                : null;
    }

    private static final JsonMapper createDefaultMapper() {
        var builder = JsonMapper.builder()
                .changeDefaultPropertyInclusion(old -> old.withValueInclusion(NON_ABSENT).withContentInclusion(NON_ABSENT))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        if (KotlinModuleHolder.kotlinModule != null) {
            builder.addModule(KotlinModuleHolder.kotlinModule);
        }
        return builder.build();
    }

    private static final class JavaTypesHolder {
        private static final ConcurrentMap<Type, JavaType> CACHED = new ConcurrentHashMap<>();
    }

    private final JsonMapper jsonMapper;

    /**
     * Constructs a new {@link Jackson3JsonLibrary} with a default
     * {@link JsonMapper} and the {@link EmptyWay#NULL} value as default.
     */
    public Jackson3JsonLibrary() {
        this(createDefaultMapper());
    }

    /**
     * Constructs a new {@link Jackson3JsonLibrary} with the specified
     * {@link JsonMapper} and the {@link EmptyWay#NULL} value as default.
     *
     * @param jsonMapper the {@code JsonMapper}
     */
    public Jackson3JsonLibrary(JsonMapper jsonMapper) {
        this(jsonMapper, EmptyWay.NULL);
    }

    /**
     * Constructs a new {@link Jackson3JsonLibrary} with the specified
     * {@link JsonMapper} and the specified {@link EmptyWay} given.
     *
     * @param jsonMapper the {@code JsonMapper}
     * @param emptyWay   the {@code EmptyWay}
     */
    public Jackson3JsonLibrary(JsonMapper jsonMapper, EmptyWay emptyWay) {
        super(emptyWay);
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T read(ByteBuf content, Type valueType) {
        try (InputStream src = new ByteBufInputStream(content.duplicate())) {
            if (valueType instanceof Class<?> clazz) {
                if (JsonNode.class.isAssignableFrom(clazz)) {
                    return (T) jsonMapper.readTree(src);
                }
                return (T) jsonMapper.readValue(src, clazz);
            }
            var javaType = JavaTypesHolder.CACHED.computeIfAbsent(valueType, jsonMapper::constructType);
            return jsonMapper.readValue(src, javaType);
        } catch (Exception e) {
            throw new JsonReadException(e.getMessage(), e);
        }
    }

    @Override
    public ByteBuf write(ByteBufAllocator alloc, Object value) {
        var buf = alloc.buffer();
        try (OutputStream out = new ByteBufOutputStream(buf)) {
            jsonMapper.writeValue(out, value);
            return buf;
        } catch (Exception e) {
            buf.release();
            throw new JsonWriteException(e.getMessage(), e);
        }
    }

}
