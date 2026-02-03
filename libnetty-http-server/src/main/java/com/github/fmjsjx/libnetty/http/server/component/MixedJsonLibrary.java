package com.github.fmjsjx.libnetty.http.server.component;

import com.github.fmjsjx.libcommon.json.Fastjson2Library;
import com.github.fmjsjx.libcommon.json.Jackson3Library;
import com.github.fmjsjx.libcommon.json.JsonDecoder;
import com.github.fmjsjx.libcommon.json.JsonEncoder;
import com.github.fmjsjx.libcommon.util.ReflectUtil;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * The mixed implementation of {@link JsonLibrary}.
 *
 * @author MJ Fang
 * @since 3.6
 */
public class MixedJsonLibrary extends AbstractJsonLibrary {

    private static final JsonEncoder DEFAULT_ENCODER;
    private static final JsonDecoder<?> DEFAULT_DECODER;

    static {
        com.github.fmjsjx.libcommon.json.JsonLibrary<?> jsonLibrary;
        var jackson3Class = ReflectUtil.findForName("com.github.fmjsjx.libcommon.json.Jackson3Library").orElse(null);
        if (jackson3Class != null) {
            jsonLibrary = ReflectUtil.callStaticMethod(jackson3Class, "getInstance");
        } else {
            var jackson2Class = ReflectUtil.findForName("com.github.fmjsjx.libcommon.json.Jackson2Library").orElse(null);
            if (jackson2Class != null) {
                jsonLibrary = ReflectUtil.callStaticMethod(jackson2Class, "getInstance");
            } else {
                var fastjson2Class = ReflectUtil.findForName("com.github.fmjsjx.libcommon.json.Fastjson2Library").orElse(null);
                if (fastjson2Class != null) {
                    jsonLibrary = ReflectUtil.callStaticMethod(fastjson2Class, "getInstance");
                } else {
                    var jsoniterClass = ReflectUtil.findForName("com.github.fmjsjx.libcommon.json.JsoniterLibrary").orElse(null);
                    if (jsoniterClass != null) {
                        jsonLibrary = ReflectUtil.callStaticMethod(jsoniterClass, "getInstance");
                    } else {
                        throw new UnsupportedOperationException("No JSON library found");
                    }
                }
            }
        }
        DEFAULT_ENCODER = jsonLibrary;
        DEFAULT_DECODER = jsonLibrary;
    }

    /**
     * The builder builds {@link MixedJsonLibrary}s.
     *
     * @author MJ Fang
     * @since 3.9
     */
    public static final class Builder {

        /**
         * Create and returns a new {@link Builder} with the default
         * parameters.
         *
         * @return a new {@link Builder}
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * Creates and returns a new {@link Builder} with the
         * recommended combination, the {@link Fastjson2Library} as the
         * encoder and the {@link Fastjson2Library} as the decoder.
         *
         * @return a new {@link Builder}
         */
        public static Builder recommended() {
            return builder().codec(Fastjson2Library.getInstance());
        }

        private JsonEncoder encoder = DEFAULT_ENCODER;
        private JsonDecoder<?> decoder = DEFAULT_DECODER;
        private EmptyWay emptyWay = EmptyWay.NULL;
        private BiFunction<HttpRequestContext, String, String> beforeRead;
        private BiFunction<HttpRequestContext, Object, Object> beforeEncode;
        private BiFunction<HttpRequestContext, String, String> beforeWrite;

        private Builder() {
        }

        /**
         * Sets the specified {@link JsonEncoder} and returns this builder.
         * <p>
         * The default is {@link Jackson3Library}.
         *
         * @param encoder the {@link JsonEncoder}
         * @return this {@link Builder}
         */
        public Builder encoder(JsonEncoder encoder) {
            this.encoder = requireNonNull(encoder, "the encoder must not be null");
            return this;
        }

        /**
         * Sets the specified {@link JsonDecoder} and returns this builder.
         * <p>
         * The default is {@link Jackson3Library}.
         *
         * @param decoder the {@link JsonDecoder}
         * @return this {@link Builder}
         */
        public Builder decoder(JsonDecoder<?> decoder) {
            this.decoder = requireNonNull(decoder, "the decoder must not be null");
            return this;
        }

        /**
         * Sets the specified codec and returns this builder.
         * <p>
         * The default is {@link Jackson3Library}.
         *
         * @param codec the {@link com.github.fmjsjx.libcommon.json.JsonLibrary}
         * @return this {@link Builder}
         * @since 4.1
         */
        public Builder codec(com.github.fmjsjx.libcommon.json.JsonLibrary<?> codec) {
            return encoder(codec).decoder(codec);
        }

        /**
         * Sets the specified {@link EmptyWay} and returns this builder.
         * <p>
         * The default is {@link EmptyWay#NULL}.
         *
         * @param emptyWay the {@link EmptyWay}
         * @return this {@link Builder}
         */
        public Builder emptyWay(EmptyWay emptyWay) {
            this.emptyWay = requireNonNull(emptyWay, "the emptyWay must not be null");
            return this;
        }

        /**
         * Sets the {@code beforeRead} function that will be executed
         * before the JSON string be read.
         * <p>
         * The non-null string returned by the function will replace the
         * original JSON string to be read by the {@link JsonLibrary}.
         *
         * @param beforeRead the function that will be executed before
         *                   the JSON string be read.
         * @return this {@link Builder}
         */
        public Builder beforeRead(BiFunction<HttpRequestContext, String, String> beforeRead) {
            this.beforeRead = beforeRead;
            return this;
        }

        /**
         * Sets the {@code beforeEncode} function that will be executed
         * before the object be encoded to JSON string.
         * <p>
         * The non-null object returned by the function will replace the
         * original object to be encoded to JSON string.
         *
         * @param beforeEncode the function that will be executed before
         *                     the object be encoded to JSON string
         * @return this {@link Builder}
         */
        public Builder beforeEncode(BiFunction<HttpRequestContext, Object, Object> beforeEncode) {
            this.beforeEncode = beforeEncode;
            return this;
        }

        /**
         * Sets the {@code beforeWrite} function that will be executed
         * before the JSON string be written to the response.
         * <p>
         * The non-null string returned by the function will replace the
         * original JSON string to be written to the response.
         *
         * @param beforeWrite the function that will be executed before
         *                    the JSON string be written to the response
         * @return this {@link Builder}
         */
        public Builder beforeWrite(BiFunction<HttpRequestContext, String, String> beforeWrite) {
            this.beforeWrite = beforeWrite;
            return this;
        }

        /**
         * Creates and returns a new {@link MixedJsonLibrary} instance.
         *
         * @return a new {@link MixedJsonLibrary} instance
         */
        public MixedJsonLibrary build() {
            var reader = buildJsonReader();
            var writer = buildJsonWriter();
            return new MixedJsonLibrary(emptyWay, encoder, decoder, reader, writer);
        }

        private JsonReader buildJsonReader() {
            var jsonDecoder = this.decoder;
            var beforeRead = this.beforeRead;
            if (beforeRead != null) {
                return (ctx, valueType) -> {
                    var content = ctx.request().content().toString(CharsetUtil.UTF_8);
                    try {
                        var replaced = beforeRead.apply(ctx, content);
                        return jsonDecoder.loads(replaced != null ? replaced : content, valueType);
                    } catch (Exception e) {
                        throw new JsonReadException(e.getMessage(), e);
                    }
                };
            }
            return (ctx, valueType) -> {
                var bytes = ByteBufUtil.getBytes(ctx.request().content());
                try {
                    return jsonDecoder.loads(bytes, valueType);
                } catch (Exception e) {
                    throw new JsonReadException(e.getMessage(), e);
                }
            };
        }

        private JsonWriter buildJsonWriter() {
            var jsonEncoder = this.encoder;
            var beforeEncode = this.beforeEncode;
            var beforeWrite = this.beforeWrite;
            if (beforeWrite != null) {
                if (beforeEncode != null) {
                    return (ctx, value) -> {
                        var object = doBeforeEncode(ctx, value, beforeEncode);
                        var bodyBytes = encodeJsonBody(ctx, object, jsonEncoder, beforeWrite);
                        return ctx.alloc().buffer(bodyBytes.length).writeBytes(bodyBytes);
                    };
                }
                return (ctx, value) -> {
                    var bodyBytes = encodeJsonBody(ctx, value, jsonEncoder, beforeWrite);
                    return ctx.alloc().buffer(bodyBytes.length).writeBytes(bodyBytes);
                };
            }
            if (beforeEncode != null) {
                return (ctx, value) -> {
                    var object = doBeforeEncode(ctx, value, beforeEncode);
                    return encodeJsonBody(ctx, object, jsonEncoder);
                };
            }
            return (ctx, value) -> encodeJsonBody(ctx, value, jsonEncoder);
        }

    }

    private static Object doBeforeEncode(HttpRequestContext ctx, Object value,
                                         BiFunction<HttpRequestContext, Object, Object> beforeEncode) {
        if (value != null) {
            try {
                var object = beforeEncode.apply(ctx, value);
                return object != null ? object : value;
            } catch (Exception e) {
                throw new JsonWriteException(e.getMessage(), e);
            }
        }
        return null;
    }

    private static byte[] encodeJsonBody(HttpRequestContext ctx, Object value, JsonEncoder jsonEncoder,
                                         BiFunction<HttpRequestContext, String, String> beforeWrite) {
        try {
            var content = jsonEncoder.dumpsToString(value);
            var replaced = beforeWrite.apply(ctx, content);
            var body = replaced != null ? replaced : content;
            return body.getBytes(CharsetUtil.UTF_8);
        } catch (Exception e) {
            throw new JsonWriteException(e.getMessage(), e);
        }
    }

    private static ByteBuf encodeJsonBody(HttpRequestContext ctx, Object value, JsonEncoder jsonEncoder) {
        var buf = ctx.alloc().buffer();
        try (OutputStream out = new ByteBufOutputStream(buf)) {
            jsonEncoder.dumps(value, out);
            return buf;
        } catch (Exception e) {
            buf.release();
            throw new JsonWriteException(e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface JsonReader {
        Object read(HttpRequestContext ctx, Type valueType);
    }

    @FunctionalInterface
    private interface JsonWriter {
        ByteBuf write(HttpRequestContext ctx, Object value);
    }

    /**
     * Create and returns a new {@link Builder} with the default
     * parameters.
     *
     * @return a new {@link Builder} with the default parameters
     * @since 3.9
     */
    public static Builder builder() {
        return Builder.create();
    }

    /**
     * Creates and returns a new {@link MixedJsonLibrary} instance with the
     * recommended combination, the {@link Fastjson2Library} as the encoder
     * and the {@link Fastjson2Library} as the decoder.
     *
     * @return a {@code MixedJsonLibrary}
     */
    public static MixedJsonLibrary recommended() {
        return recommended(EmptyWay.NULL);
    }

    /**
     * Creates and returns a new {@link MixedJsonLibrary} instance with the
     * recommended combination, the {@link Fastjson2Library} as the encoder
     * and the {@link Fastjson2Library} as the decoder.
     *
     * @param emptyWay the {@code EmptyWay}
     * @return a {@code MixedJsonLibrary}
     */
    public static MixedJsonLibrary recommended(EmptyWay emptyWay) {
        return Builder.recommended().emptyWay(emptyWay).build();
    }

    private final JsonEncoder encoder;
    private final JsonDecoder<?> decoder;
    private final JsonReader reader;
    private final JsonWriter writer;

    private MixedJsonLibrary(EmptyWay emptyWay, JsonEncoder encoder, JsonDecoder<?> decoder, JsonReader reader,
                             JsonWriter writer) {
        super(emptyWay);
        this.encoder = requireNonNull(encoder, "the encoder must not be null");
        this.decoder = requireNonNull(decoder, "the decoder must not be null");
        this.reader = requireNonNull(reader, "the reader must not be null");
        this.writer = requireNonNull(writer, "the writer must not be null");
    }

    /**
     * Construct with the specified {@link JsonEncoder} ane the specified
     * {@link JsonDecoder} and the specified {@link EmptyWay} given.
     *
     * @param encoder  the {@code JsonEncoder}
     * @param decoder  the {@code JsonDecoder}
     * @param emptyWay the {@code EmptyWay}
     * @deprecated since 3.9, please use {@link Builder} instead
     */
    @Deprecated(since = "3.9")
    public MixedJsonLibrary(JsonEncoder encoder, JsonDecoder<?> decoder, EmptyWay emptyWay) {
        super(emptyWay);
        this.encoder = requireNonNull(encoder, "encoder must not be null");
        this.decoder = requireNonNull(decoder, "decoder must not be null");
        this.reader = super::read;
        this.writer = super::write;
    }

    /**
     * Construct with the specified {@link JsonEncoder} ane the specified
     * {@link JsonDecoder} and the default {@link EmptyWay} {@code NULL}.
     *
     * @param encoder the {@code JsonEncoder}
     * @param decoder the {@code JsonDecoder}
     * @deprecated since 3.9, please use {@link Builder} instead
     */
    @Deprecated(since = "3.9")
    public MixedJsonLibrary(JsonEncoder encoder, JsonDecoder<?> decoder) {
        this(encoder, decoder, EmptyWay.NULL);
    }

    @Override
    public <T> T read(ByteBuf content, Type valueType) {
        var bytes = ByteBufUtil.getBytes(content);
        try {
            return decoder.loads(bytes, valueType);
        } catch (Exception e) {
            throw new JsonReadException(e.getMessage(), e);
        }
    }

    @Override
    public ByteBuf write(@NotNull ByteBufAllocator alloc, Object value) {
        var buf = alloc.buffer();
        try (OutputStream out = new ByteBufOutputStream(buf)) {
            encoder.dumps(value, out);
            return buf;
        } catch (Exception e) {
            buf.release();
            throw new JsonWriteException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T read(HttpRequestContext ctx, Type valueType) {
        return (T) reader.read(ctx, valueType);
    }

    @Override
    public ByteBuf write(HttpRequestContext ctx, Object value) {
        return writer.write(ctx, value);
    }

}
