package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;

/**
 * A FastCGI request.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiRequest extends AbstractReferenceCounted implements FcgiMessage {

    private final FcgiVersion protocolVersion;
    private final int requestId;

    private final FcgiBeginRequest beginRequest;
    private final FcgiParams params;
    private final FcgiStdin stdin;
    private final Optional<FcgiData> data;

    public FcgiRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role, ByteBuf stdinContent) {
        this(protocolVersion, requestId, role, stdinContent, null);
    }

    public FcgiRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role, ByteBuf stdinContent,
            ByteBuf dataContent) {
        this.protocolVersion = protocolVersion;
        this.requestId = requestId;
        this.beginRequest = new FcgiBeginRequest(protocolVersion, requestId, role);
        this.params = new FcgiParams(protocolVersion, requestId);
        this.stdin = new FcgiStdin(protocolVersion, requestId, stdinContent);
        if (dataContent != null) {
            this.data = Optional.of(new FcgiData(protocolVersion, requestId, dataContent));
        } else {
            this.data = Optional.empty();
        }
    }

    public FcgiVersion protocolVersion() {
        return protocolVersion;
    }

    public int requestId() {
        return requestId;
    }

    public FcgiBeginRequest beginRequest() {
        return beginRequest;
    }

    public FcgiParams params() {
        return params;
    }

    public FcgiStdin stdin() {
        return stdin;
    }

    public Optional<FcgiData> data() {
        return data;
    }

    @Override
    public FcgiRequest touch(Object hint) {
        stdin.touch(hint);
        data.ifPresent(d -> d.touch(hint));
        return this;
    }

    @Override
    protected void deallocate() {
        stdin.release();
        data.ifPresent(FcgiData::release);
    }

    @Override
    public FcgiRequest retain() {
        super.retain();
        return this;
    }

    @Override
    public FcgiRequest retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public FcgiRequest touch() {
        super.touch();
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(beginRequest).append('\n').append(params).append('\n').append(stdin);
        data.ifPresent(d -> builder.append('\n').append(d));
        return builder.toString();
    }

}
