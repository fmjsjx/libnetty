package com.github.fmjsjx.libnetty.http.server.component;

import com.github.fmjsjx.libcommon.json.Fastjson2Library;
import com.github.fmjsjx.libcommon.json.JsonDecoder;
import com.github.fmjsjx.libcommon.json.JsonEncoder;
import com.github.fmjsjx.libcommon.json.JsoniterLibrary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * The mixed implementation of {@link JsonLibrary}.
 *
 * @author MJ Fang
 * @since 3.6
 */
public class MixedJsonLibrary extends AbstractJsonLibrary {

    /**
     * Creates and returns a new {@link MixedJsonLibrary} instance with the
     * recommended combination, the {@link Fastjson2Library} as the encoder
     * and the {@link JsoniterLibrary} as the decoder.
     *
     * @return a {@code MixedJsonLibrary}
     */
    public static MixedJsonLibrary recommended() {
        return recommended(EmptyWay.NULL);
    }

    /**
     * Creates and returns a new {@link MixedJsonLibrary} instance with the
     * recommended combination, the {@link Fastjson2Library} as the encoder
     * and the {@link JsoniterLibrary} as the decoder.
     *
     * @param emptyWay the {@code EmptyWay}
     * @return a {@code MixedJsonLibrary}
     */
    public static MixedJsonLibrary recommended(EmptyWay emptyWay) {
        return new MixedJsonLibrary(Fastjson2Library.getInstance(), JsoniterLibrary.getInstance(), emptyWay);
    }

    private final JsonEncoder encoder;
    private final JsonDecoder<?> decoder;

    /**
     * Construct with the specified {@link JsonEncoder} ane the specified
     * {@link JsonDecoder} and the specified {@link EmptyWay} given.
     *
     * @param encoder  the {@code JsonEncoder}
     * @param decoder  the {@code JsonDecoder}
     * @param emptyWay the {@code EmptyWay}
     */
    public MixedJsonLibrary(JsonEncoder encoder, JsonDecoder<?> decoder, EmptyWay emptyWay) {
        super(emptyWay);
        this.encoder = Objects.requireNonNull(encoder, "encoder must not be null");
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
    }

    /**
     * Construct with the specified {@link JsonEncoder} ane the specified
     * {@link JsonDecoder} and the default {@link EmptyWay} {@code NULL}.
     *
     * @param encoder  the {@code JsonEncoder}
     * @param decoder  the {@code JsonDecoder}
     */
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
    public ByteBuf write(ByteBufAllocator alloc, Object value) {
        var buf = alloc.buffer();
        try (OutputStream out = new ByteBufOutputStream(buf)) {
            encoder.dumps(value, out);
            return buf;
        } catch (Exception e) {
            buf.release();
            throw new JsonWriteException(e.getMessage(), e);
        }
    }

}
