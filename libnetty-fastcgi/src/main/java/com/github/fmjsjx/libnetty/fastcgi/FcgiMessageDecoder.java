package com.github.fmjsjx.libnetty.fastcgi;

import static com.github.fmjsjx.libnetty.fastcgi.FcgiConstants.*;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Decodes {@link ByteBuf}s to {@link FcgiMessage}s.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            for (;;) {
                if (!in.isReadable(FCGI_HEADER_LEN)) {
                    return;
                }
                int contentLength = FcgiCodecUtil.getContentLength(in);
                int paddingLength = FcgiCodecUtil.getPaddingLength(in);
                if (!in.isReadable(FCGI_HEADER_LEN + contentLength + paddingLength)) {
                    return;
                }
                FcgiVersion version = FcgiVersion.valueOf(FcgiCodecUtil.getVersion(in));
                int requestId = FcgiCodecUtil.getVersion(in);
                FcgiRecordType type = FcgiRecordType.valueOf(FcgiCodecUtil.getType(in));
                if (type.isUnknown()) {
                    FcgiUnknownType unknownType = new FcgiUnknownType(version, requestId, type.type());
                    ctx.writeAndFlush(unknownType);
                    in.skipBytes(FCGI_HEADER_LEN + contentLength + paddingLength);
                }
                // TODO Auto-generated method stub
            }
        } catch (Exception e) {
            throw e instanceof FcgiDecoderException ? e : new FcgiDecoderException(e);
        }
    }

}
