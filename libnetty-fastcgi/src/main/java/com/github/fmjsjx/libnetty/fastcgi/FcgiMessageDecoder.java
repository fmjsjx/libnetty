package com.github.fmjsjx.libnetty.fastcgi;

import static com.github.fmjsjx.libnetty.fastcgi.FcgiConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

/**
 * Decodes {@link ByteBuf}s to {@link FcgiMessage}s.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiMessageDecoder extends ByteToMessageDecoder {

    private final IntObjectMap<FcgiMessageBuilder<?, ?>> map = new IntObjectHashMap<>();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            for (;;) {
                if (!in.isReadable(FCGI_HEADER_LEN)) {
                    return;
                }
                int contentLength = FcgiCodecUtil.getContentLength(in);
                int paddingLength = FcgiCodecUtil.getPaddingLength(in);
                int fullLength = FCGI_HEADER_LEN + contentLength + paddingLength;
                if (!in.isReadable(fullLength)) {
                    return;
                }
                FcgiVersion version = FcgiVersion.valueOf(FcgiCodecUtil.getVersion(in));
                int requestId = FcgiCodecUtil.getVersion(in);
                FcgiRecordType type = FcgiRecordType.valueOf(FcgiCodecUtil.getType(in));
                ByteBuf content = contentLength == 0 ? Unpooled.EMPTY_BUFFER
                        : in.slice(in.readerIndex() + FCGI_HEADER_LEN, contentLength);
                in.skipBytes(fullLength);
                if (type == FcgiRecordType.BEGIN_REQUEST) {
                    FcgiBeginRequest beginRequest = decodeBeginRequest(content, version, requestId);
                    FcgiRequestBuilder builder = new FcgiRequestBuilder(beginRequest);
                    map.put(requestId, builder);
                } else if (type == FcgiRecordType.ABORT_REQUEST) {
                    FcgiAbortRequest abortRequest = new FcgiAbortRequest(version, requestId);
                    out.add(abortRequest);
                } else if (type == FcgiRecordType.END_REQUEST) {
                    FcgiEndRequest endRequest = decodeEndRequest(version, requestId, content);
                    FcgiMessageBuilder<?, ?> builder = map.remove(requestId);
                    if (builder == null) {
                        // WARN: No FCGI_STDOUT or FCGI_STDERR received before,
                        // just raise the empty values response.
                        FcgiResponse response = new FcgiResponse(endRequest, new FcgiStdout(version, requestId), null);
                        out.add(response);
                    } else {
                        out.add(builder.endRequest(endRequest).build());
                    }
                } else if (type == FcgiRecordType.PARAMS) {
                    FcgiMessageBuilder<?, ?> builder = getAndEnsureRequestBuilder(requestId);
                    if (contentLength > 0) {
                        builder.params(content.retain());
                    } else if (builder.beginRequest().role() == FcgiRole.AUTHORIZER) {
                        // finish request for role => FCGI_AUTHORIZER
                        map.remove(requestId);
                        out.add(builder.build());
                    }
                } else if (type == FcgiRecordType.STDIN) {
                    FcgiMessageBuilder<?, ?> builder = getAndEnsureRequestBuilder(requestId);
                    if (contentLength > 0) {
                        builder.stdin(content.retain());
                    } else if (builder.beginRequest().role() == FcgiRole.RESPONDER) {
                        // finish request for role => FCGI_RESPONDER
                        map.remove(requestId);
                        out.add(builder.build());
                    }
                } else if (type == FcgiRecordType.STDOUT) {
                    FcgiMessageBuilder<?, ?> builder = map.get(requestId);
                    if (builder == null) {
                        builder = new FcgiResponseBuilder(version, requestId);
                        map.put(requestId, builder);
                    }
                    if (contentLength > 0) {
                        builder.stdout(content.retain());
                    }
                } else if (type == FcgiRecordType.STDERR) {
                    FcgiMessageBuilder<?, ?> builder = map.get(requestId);
                    if (builder == null) {
                        // WARN: No FCGI_STDOUT received before
                        builder = new FcgiResponseBuilder(version, requestId);
                        map.put(requestId, builder);
                    }
                    if (contentLength > 0) {
                        builder.stderr(content.retain());
                    }
                } else if (type == FcgiRecordType.DATA) {
                    FcgiMessageBuilder<?, ?> builder = getAndEnsureRequestBuilder(requestId);
                    if (contentLength > 0) {
                        builder.data(content.retain());
                    } else if (builder.beginRequest().role() == FcgiRole.FILTER) {
                        // finish request for role => FCGI_FILTER
                        map.remove(requestId);
                        out.add(builder.build());
                    }
                } else if (type == FcgiRecordType.GET_VALUES) {
                    FcgiGetValues getValues = new FcgiGetValues(version);
                    for (; content.isReadable();) {
                        int nameLength = FcgiCodecUtil.decodeVariableLength(content);
                        int valueLength = FcgiCodecUtil.decodeVariableLength(content);
                        String name = content.readCharSequence(nameLength, CharsetUtil.UTF_8).toString();
                        if (valueLength > 0) {
                            content.skipBytes(valueLength);
                        }
                        getValues.put(name);
                    }
                    out.add(getValues);
                } else if (type == FcgiRecordType.GET_VALUES_RESULT) {
                    FcgiGetValuesResult getValuesResult = new FcgiGetValuesResult(version);
                    for (; content.isReadable();) {
                        int nameLength = FcgiCodecUtil.decodeVariableLength(content);
                        int valueLength = FcgiCodecUtil.decodeVariableLength(content);
                        String name = content.readCharSequence(nameLength, CharsetUtil.UTF_8).toString();
                        if (valueLength > 0) {
                            String value = content.readCharSequence(valueLength, CharsetUtil.UTF_8).toString();
                            getValuesResult.put(name, value);
                        } else {
                            getValuesResult.put(name, "");
                        }
                    }
                    out.add(getValuesResult);
                } else if (type == FcgiRecordType.UNKNOWN_TYPE) {
                    FcgiUnknownType unknownType = decodeUnknownType(version, requestId, content);
                    out.add(unknownType);
                } else if (type.isUnknown()) {
                    FcgiUnknownType unknownType = new FcgiUnknownType(version, requestId, type.type());
                    ctx.writeAndFlush(unknownType);
                }
                // no other case
            }
        } catch (Exception e) {
            throw e instanceof FcgiDecoderException ? e : new FcgiDecoderException(e);
        }
    }

    private FcgiMessageBuilder<?, ?> getAndEnsureRequestBuilder(int requestId) {
        FcgiMessageBuilder<?, ?> builder = map.get(requestId);
        if (builder == null) {
            throw new FcgiDecoderException("no FCGI_BEGIN_REQUEST with request id " + requestId + " received before");
        }
        return builder;
    }

    private static final FcgiBeginRequest decodeBeginRequest(ByteBuf in, FcgiVersion version, int requestId) {
        FcgiRole role = FcgiRole.valueOf(in.readUnsignedShort());
        int flags = in.readUnsignedByte();
        FcgiBeginRequest beginRequest = new FcgiBeginRequest(version, requestId, role, flags);
        return beginRequest;
    }

    private static final FcgiEndRequest decodeEndRequest(FcgiVersion version, int requestId, ByteBuf content) {
        int appStatus = content.readInt();
        FcgiProtocolStatus protocolStatus = FcgiProtocolStatus.valueOf(content.readUnsignedByte());
        FcgiEndRequest endRequest = new FcgiEndRequest(version, requestId, appStatus, protocolStatus);
        return endRequest;
    }

    private static final FcgiUnknownType decodeUnknownType(FcgiVersion version, int requestId, ByteBuf content) {
        return new FcgiUnknownType(version, requestId, content.readUnsignedByte());
    }

    static abstract class FcgiMessageBuilder<T extends FcgiMessage, Self extends FcgiMessageBuilder<?, ?>> {

        protected final FcgiVersion protocolVersion;
        protected final int requestId;

        protected FcgiMessageBuilder(FcgiVersion protocolVersion, int requestId) {
            this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion must not be null");
            this.requestId = requestId;
        }

        FcgiBeginRequest beginRequest() {
            throw new UnsupportedOperationException();
        }

        Self beginRequest(FcgiBeginRequest beginRequest) {
            throw new UnsupportedOperationException();
        }

        Self params(ByteBuf content) {
            throw new UnsupportedOperationException();
        }

        Self stdin(ByteBuf content) {
            throw new UnsupportedOperationException();
        }

        Self data(ByteBuf content) {
            throw new UnsupportedOperationException();
        }

        Self stdout(ByteBuf content) {
            throw new UnsupportedOperationException();
        }

        Self stderr(ByteBuf content) {
            throw new UnsupportedOperationException();
        }

        FcgiEndRequest endRequest() {
            throw new UnsupportedOperationException();
        }

        Self endRequest(FcgiEndRequest endRequest) {
            throw new UnsupportedOperationException();
        }

        ByteBuf wrapped(List<ByteBuf> bufs) {
            return Unpooled.wrappedBuffer(bufs.stream().toArray(ByteBuf[]::new));
        }

        abstract T build();
    }

    static final class FcgiRequestBuilder extends FcgiMessageBuilder<FcgiRequest, FcgiRequestBuilder> {

        private FcgiBeginRequest beginRequest;
        private final List<ByteBuf> paramsBufs = new ArrayList<ByteBuf>();
        private final List<ByteBuf> stdinBufs = new ArrayList<ByteBuf>();
        private List<ByteBuf> dataBufs;

        FcgiRequestBuilder(FcgiBeginRequest beginRequest) {
            super(beginRequest.protocolVersion(), beginRequest.requestId());
            beginRequest(beginRequest);
        }

        @Override
        FcgiBeginRequest beginRequest() {
            return beginRequest;
        }

        @Override
        FcgiRequestBuilder beginRequest(FcgiBeginRequest beginRequest) {
            this.beginRequest = beginRequest;
            return this;
        }

        @Override
        FcgiRequestBuilder params(ByteBuf content) {
            paramsBufs.add(content);
            return this;
        }

        @Override
        FcgiRequestBuilder stdin(ByteBuf content) {
            stdinBufs.add(content);
            return this;
        }

        @Override
        FcgiRequestBuilder data(ByteBuf content) {
            if (dataBufs == null) {
                dataBufs = new ArrayList<ByteBuf>();
            }
            dataBufs.add(content);
            return this;
        }

        @Override
        FcgiRequest build() {
            FcgiParams params = buildParams();
            FcgiStdin stdin = new FcgiStdin(protocolVersion, requestId, wrapped(stdinBufs));
            FcgiData data = null;
            if (dataBufs != null) {
                data = new FcgiData(protocolVersion, requestId, wrapped(dataBufs));
            }
            return new FcgiRequest(beginRequest, params, stdin, data);
        }

        private FcgiParams buildParams() {
            FcgiParams params = new FcgiParams(protocolVersion, requestId);
            if (paramsBufs.size() > 0) {
                ByteBuf content = wrapped(paramsBufs);
                try {
                    for (; content.isReadable();) {
                        int nameLength = FcgiCodecUtil.decodeVariableLength(content);
                        int valueLength = FcgiCodecUtil.decodeVariableLength(content);
                        String name = content.readCharSequence(nameLength, CharsetUtil.UTF_8).toString();
                        if (valueLength > 0) {
                            String value = content.readCharSequence(valueLength, CharsetUtil.UTF_8).toString();
                            params.put(name, value);
                        } else {
                            params.put(name, "");
                        }
                    }
                } finally {
                    content.release();
                }
            }
            return params;
        }

    }

    static final class FcgiResponseBuilder extends FcgiMessageBuilder<FcgiResponse, FcgiResponseBuilder> {

        private FcgiEndRequest endRequest;
        private final List<ByteBuf> stdoutBufs = new ArrayList<ByteBuf>();
        private List<ByteBuf> stderrBufs;

        FcgiResponseBuilder(FcgiVersion version, int requestId) {
            super(version, requestId);
        }

        @Override
        FcgiResponseBuilder stdout(ByteBuf content) {
            stdoutBufs.add(content);
            return this;
        }

        @Override
        FcgiResponseBuilder stderr(ByteBuf content) {
            if (stderrBufs == null) {
                stderrBufs = new ArrayList<>();
            }
            stderrBufs.add(content);
            return this;
        }

        @Override
        FcgiEndRequest endRequest() {
            return endRequest;
        }

        @Override
        FcgiResponseBuilder endRequest(FcgiEndRequest endRequest) {
            this.endRequest = endRequest;
            return this;
        }

        @Override
        FcgiResponse build() {
            FcgiStdout stdout = new FcgiStdout(protocolVersion, requestId, wrapped(stdoutBufs));
            FcgiStderr stderr = null;
            if (stderrBufs != null) {
                stderr = new FcgiStderr(protocolVersion, requestId, wrapped(stderrBufs));
            }
            return new FcgiResponse(endRequest, stdout, stderr);
        }

    }

}
