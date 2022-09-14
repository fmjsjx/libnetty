package com.github.fmjsjx.libnetty.http.client;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

import java.io.IOException;
import java.util.List;

import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentDecompressor;

/**
 * Decompresses an {@link FullHttpResponse} compressed in {@code br(Brotli)}
 * encoding.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 * @deprecated {@link HttpContentDecompressor} already support {@code br(Brotli)}
 */
@Sharable
@Deprecated
public class BrotliDecompressor extends MessageToMessageDecoder<FullHttpResponse> {

    private static final int MAX_BUF_LEN = 65536;

    private static final int MIN_BUF_LEN = 4096;

    private static final Logger logger = LoggerFactory.getLogger(BrotliDecompressor.class);

    /**
     * The singleton {@link BrotliDecompressor} instance.
     */
    static final BrotliDecompressor INSTANCE = new BrotliDecompressor();

    /**
     * Returns the singleton {@link BrotliDecompressor} instance.
     * 
     * @return the singleton {@link BrotliDecompressor} instance
     */
    public static final BrotliDecompressor getInstance() {
        return INSTANCE;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpResponse msg, List<Object> out) throws Exception {
        if (!msg.headers().containsValue(CONTENT_ENCODING, "br", true)) {
            ctx.fireChannelRead(msg.retain());
            return;
        }
        logger.debug("Decompress HTTP content compressed in Brotli: {}", msg);
        int baseLength = msg.content().readableBytes();
        if (baseLength == 0) {
            msg.headers().remove(CONTENT_ENCODING).setInt(CONTENT_LENGTH, 0);
            ctx.fireChannelRead(msg.retain());
            return;
        }
        try (BrotliInputStream in = new BrotliInputStream(new ByteBufInputStream(msg.content()))) {
            int estimated = Math.max(MIN_BUF_LEN, ctx.alloc().calculateNewCapacity(baseLength << 1, Integer.MAX_VALUE));
            ByteBuf content = ctx.alloc().heapBuffer(estimated);
            byte[] destBuffer = new byte[Math.min(MAX_BUF_LEN, estimated)];
            for (;;) {
                int len = in.read(destBuffer);
                if (len == -1) {
                    break;
                }
                content.writeBytes(destBuffer, 0, len);
            }
            int contentLength = content.readableBytes();
            FullHttpResponse resp = msg.replace(content);
            resp.headers().remove(CONTENT_ENCODING).setInt(CONTENT_LENGTH, contentLength);
            logger.debug("Decompressed HTTP response in Brotli: {}", resp);
            out.add(resp);
        } catch (IOException e) {
            throw new BrotliDecoderException(e);
        }
    }

    /**
     * An {@link DecoderException} which is thrown by a {@link BrotliDecompressor}.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     */
    public static final class BrotliDecoderException extends DecoderException {

        private static final long serialVersionUID = 1334042212343437566L;

        /**
         * Creates a new instance.
         * 
         * @param cause the cause
         */
        public BrotliDecoderException(Throwable cause) {
            super(cause);
        }

    }

}
