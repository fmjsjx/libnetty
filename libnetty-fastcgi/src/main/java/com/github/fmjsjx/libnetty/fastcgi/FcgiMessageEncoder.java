package com.github.fmjsjx.libnetty.fastcgi;

import static com.github.fmjsjx.libnetty.fastcgi.FcgiConstants.*;

import java.util.List;

import com.github.fmjsjx.libnetty.fastcgi.FcgiNameValuePairs.NameValuePair;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

/**
 * Encodes {@link FcgiMessage}s to {@link ByteBuf}s.
 * 
 * <p>
 * This encoder is {@code sharable}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
@Sharable
public class FcgiMessageEncoder extends MessageToMessageEncoder<FcgiMessage> {

    private static final ByteBuf[] paddingBufs = new ByteBuf[256];

    private static final ByteBuf createPaddingBuf(int paddingLength) {
        return Unpooled.unreleasableBuffer(
                ByteBufAllocator.DEFAULT.buffer(paddingLength, paddingLength).writeZero(paddingLength).asReadOnly());
    }

    static {
        paddingBufs[0] = Unpooled.EMPTY_BUFFER; // 0
        for (int i = 1; i < 8; i++) {
            paddingBufs[i] = createPaddingBuf(i);
        }
    }

    private static final ByteBuf paddingBuf(int paddingLength) {
        ByteBuf buf = paddingBufs[paddingLength];
        if (buf == null) {
            synchronized (paddingBufs) {
                buf = paddingBufs[paddingLength];
                if (buf == null) {
                    paddingBufs[paddingLength] = buf = createPaddingBuf(paddingLength);
                }
            }
        }
        return buf;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FcgiMessage msg, List<Object> out) throws Exception {
        if (msg instanceof FcgiRequest) {
            encode(ctx, (FcgiRequest) msg, out);
        } else if (msg instanceof FcgiResponse) {
            encode(ctx, (FcgiResponse) msg, out);
        } else if (msg instanceof FcgiAbortRequest) {
            encode(ctx, (FcgiAbortRequest) msg, out);
        } else if (msg instanceof FcgiUnknownType) {
            encode(ctx, (FcgiUnknownType) msg, out);
        } else if (msg instanceof FcgiGetValues) {
            encode(ctx, (FcgiGetValues) msg, out);
        } else if (msg instanceof FcgiGetValuesResult) {
            encode(ctx, (FcgiGetValuesResult) msg, out);
        }
        // no other more FcgiMessage
    }

    private static final void encode(ChannelHandlerContext ctx, FcgiRequest msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer();
        encodeFcgiRecord(msg.beginRequest(), buf);
        encodeFcgiRecord(msg.params(), buf);
        // encode FCGI_STDIN
        FcgiStdin stdin = msg.stdin();
        FcgiCodecUtil.encodeRecordHeader(stdin, buf);
        // TODO
        out.add(buf);
    }

    private static final void encodeFcgiRecord(FcgiBeginRequest beginRequest, ByteBuf buf) {
        FcgiCodecUtil.encodeRecordHeader(beginRequest, buf);
        buf.writeShort(beginRequest.role().role());
        buf.writeByte(beginRequest.flags());
        buf.writeZero(beginRequest.paddingLength());
    }

    private static final void encodeFcgiRecord(FcgiParams params, ByteBuf buf) {
        FcgiCodecUtil.encodeRecordHeaderWithoutLengths(params, buf);
        int lengthsIndex = buf.writerIndex();
        buf.writeZero(4); // skip contentLength & paddingLength on record header
        int contentLength = 0;
        for (NameValuePair pair : params.pairs()) {
            byte[] name = pair.name().getBytes(CharsetUtil.UTF_8);
            byte[] value = pair.value().getBytes(CharsetUtil.UTF_8);
            int length = FcgiCodecUtil.calculateNameValuePairLength(name, value);
            if (contentLength + length > FCGI_MAX_CONTENT_LENGTH) {
                int paddingLength = FcgiCodecUtil.calculatePaddingLength(contentLength);
                buf.writeZero(paddingLength);
                // set contentLength & paddingLength on record header
                buf.setShort(lengthsIndex, contentLength);
                buf.setByte(lengthsIndex + 2, paddingLength);
                // start next FCGI_PARAMS record
                FcgiCodecUtil.encodeRecordHeaderWithoutLengths(params, buf);
                lengthsIndex = buf.writerIndex();
                buf.writeZero(4);
                contentLength = 0;
            } else {
                contentLength += length;
                FcgiCodecUtil.encodeNameValuePair(name, value, buf);
            }
        }
        int paddingLength = FcgiCodecUtil.calculatePaddingLength(contentLength);
        buf.writeZero(paddingLength);
        // set contentLength & paddingLength on record header
        buf.setShort(lengthsIndex, contentLength);
        buf.setByte(lengthsIndex + 2, paddingLength);
        // last FCGI_PARAMS record (empty)
        FcgiCodecUtil.encodeRecordHeaderWithoutLengths(params, buf);
        FcgiCodecUtil.encodeRecordHeaderLengths(0, 0, buf);
    }

    private static final void encode(ChannelHandlerContext ctx, FcgiResponse msg, List<Object> out) throws Exception {
        // TODO
    }

    private static final void encode(ChannelHandlerContext ctx, FcgiAbortRequest msg, List<Object> out)
            throws Exception {
        ByteBuf buf = ctx.alloc().buffer(FCGI_HEADER_LEN, FCGI_HEADER_LEN);
        FcgiCodecUtil.encodeRecordHeader(msg, buf);
        out.add(buf);
    }

    private static final void encode(ChannelHandlerContext ctx, FcgiUnknownType msg, List<Object> out)
            throws Exception {
        ByteBuf buf = ctx.alloc().buffer(FCGI_HEADER_LEN + 8, FCGI_HEADER_LEN + 8);
        FcgiCodecUtil.encodeRecordHeader(msg, buf);
        buf.writeByte(msg.value());
        buf.writeZero(msg.paddingLength());
        out.add(buf);
    }

    private static final void encode(ChannelHandlerContext ctx, FcgiGetValues msg, List<Object> out) throws Exception {
        if (msg.size() == 0) {
            ByteBuf buf = ctx.alloc().buffer(FCGI_HEADER_LEN, FCGI_HEADER_LEN);
            FcgiCodecUtil.encodeRecordHeaderWithoutLengths(msg, buf);
            FcgiCodecUtil.encodeRecordHeaderLengths(0, 0, buf);
            out.add(buf);
        } else {
            ByteBuf buf = ctx.alloc().buffer();
            FcgiCodecUtil.encodeRecordHeaderWithoutLengths(msg, buf);
            int lengthsIndex = buf.writerIndex();
            buf.writeZero(4);
            for (String n : msg.names()) {
                byte[] name = n.getBytes(CharsetUtil.UTF_8);
                FcgiCodecUtil.encodeNameValuePair(name, EMPTY_VALUE, buf);
            }
            int contentLength = buf.readableBytes() - FCGI_HEADER_LEN;
            int paddingLength = FcgiCodecUtil.calculatePaddingLength(contentLength);
            buf.writeZero(paddingLength);
            buf.setShort(lengthsIndex, contentLength);
            buf.setByte(lengthsIndex + 2, paddingLength);
            out.add(buf);
        }
    }

    private static final void encode(ChannelHandlerContext ctx, FcgiGetValuesResult msg, List<Object> out)
            throws Exception {
        if (msg.size() == 0) {
            ByteBuf buf = ctx.alloc().buffer(FCGI_HEADER_LEN, FCGI_HEADER_LEN);
            FcgiCodecUtil.encodeRecordHeaderWithoutLengths(msg, buf);
            FcgiCodecUtil.encodeRecordHeaderLengths(0, 0, buf);
            out.add(buf);
        } else {
            ByteBuf buf = ctx.alloc().buffer();
            FcgiCodecUtil.encodeRecordHeaderWithoutLengths(msg, buf);
            int lengthsIndex = buf.writerIndex();
            buf.writeZero(4);
            for (NameValuePair pair : msg.pairs()) {
                byte[] name = pair.name().getBytes(CharsetUtil.UTF_8);
                byte[] value = pair.value().getBytes(CharsetUtil.UTF_8);
                FcgiCodecUtil.encodeNameValuePair(name, value, buf);
            }
            int contentLength = buf.readableBytes() - FCGI_HEADER_LEN;
            int paddingLength = FcgiCodecUtil.calculatePaddingLength(contentLength);
            buf.writeZero(paddingLength);
            buf.setShort(lengthsIndex, contentLength);
            buf.setByte(lengthsIndex + 2, paddingLength);
            out.add(buf);
        }
    }

}
