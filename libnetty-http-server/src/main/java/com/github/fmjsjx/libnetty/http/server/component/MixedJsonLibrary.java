package com.github.fmjsjx.libnetty.http.server.component;

import com.github.fmjsjx.libcommon.json.JsonDecoder;
import com.github.fmjsjx.libcommon.json.JsonEncoder;
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

    private final JsonEncoder encoder;
    private final JsonDecoder<?> decoder;

    /**
     * Construct with the specified {@link EmptyWay} given.
     *
     * @param emptyWay the {@code EmptyWay}
     */
    public MixedJsonLibrary(JsonEncoder encoder, JsonDecoder<?> decoder, EmptyWay emptyWay) {
        super(emptyWay);
        this.encoder = Objects.requireNonNull(encoder, "encoder must not be null");
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
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
