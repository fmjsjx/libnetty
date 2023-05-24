package com.github.fmjsjx.libnetty.http.server.component;

import com.alibaba.fastjson2.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Implementation of {@link JsonLibrary} using {@code fastjson2}.
 *
 * @author MJ Fang
 * @since 3.1
 */
public class Fastjson2JsonLibrary implements JsonLibrary {

    private final JSONReader.Feature[] readerFeatures;
    private final JSONWriter.Feature[] writerFeatures;

    public Fastjson2JsonLibrary(JSONReader.Feature[] readerFeatures, JSONWriter.Feature[] writerFeatures) {
        this.readerFeatures = Arrays.copyOf(readerFeatures, readerFeatures.length);
        this.writerFeatures = Arrays.copyOf(writerFeatures, writerFeatures.length);
    }

    public Fastjson2JsonLibrary() {
        this(new JSONReader.Feature[0], new JSONWriter.Feature[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(ByteBuf content, Type valueType) {
        try (InputStream src = new ByteBufInputStream(content.duplicate())) {
            if (valueType instanceof Class<?> clazz) {
                if (JSONObject.class.isAssignableFrom(clazz)) {
                    return (T) JSON.parseObject(src, readerFeatures);
                } else if (JSONArray.class.isAssignableFrom(clazz)) {
                    return (T) JSON.parseArray(src, readerFeatures);
                }
                return JSON.parseObject(src, clazz, readerFeatures);
            }
            return JSON.parseObject(src, valueType, readerFeatures);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    @Override
    public ByteBuf write(ByteBufAllocator alloc, Object value) {
        ByteBuf buf = alloc.buffer();
        try (OutputStream out = new ByteBufOutputStream(buf)) {
            JSON.writeTo(out, value, writerFeatures);
            return buf;
        } catch (IOException e) {
            buf.release();
            throw new JsonException(e.getMessage(), e);
        }
    }

}
