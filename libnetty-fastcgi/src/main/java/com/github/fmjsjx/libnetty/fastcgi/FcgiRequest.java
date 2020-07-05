package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Objects;
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

    FcgiRequest(FcgiBeginRequest beginRequest, FcgiParams params, FcgiStdin stdin, FcgiData data) {
        this.beginRequest = Objects.requireNonNull(beginRequest, "beginRequest must not be null");
        this.params = Objects.requireNonNull(params, "params must not be null");
        this.stdin = Objects.requireNonNull(stdin, "stdin must not be null");
        this.data = Optional.ofNullable(data);

        this.protocolVersion = beginRequest.protocolVersion();
        this.requestId = beginRequest.requestId();
    }

    /**
     * Constructs a new {@link FcgiRequest} instance with {@code FCGI_RESPONDER}
     * role.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param stdinContent    the {@code ByteBuf} content of {@code FCGI_STDIN}
     */
    public FcgiRequest(FcgiVersion protocolVersion, int requestId, ByteBuf stdinContent) {
        this(protocolVersion, requestId, FcgiRole.RESPONDER, stdinContent, null);
    }

    /**
     * Constructs a new {@link FcgiRequest} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param role            the {@code FcgiRole}
     * @param stdinContent    the {@code ByteBuf} content of {@code FCGI_STDIN}
     */
    public FcgiRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role, ByteBuf stdinContent) {
        this(protocolVersion, requestId, role, stdinContent, null);
    }

    /**
     * Constructs a new {@link FcgiRequest} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param role            the {@code FcgiRole}
     * @param stdinContent    the {@code ByteBuf} content of {@code FCGI_STDIN}
     * @param dataContent     the {@code ByteBuf} content of {@code FCGI_DATA}
     */
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

    @Override
    public FcgiVersion protocolVersion() {
        return protocolVersion;
    }

    @Override
    public int requestId() {
        return requestId;
    }

    /**
     * Returns the {@code FCGI_BEGIN_REQUEST} record.
     * 
     * @return the {@code FcgiBeginRequest}
     */
    public FcgiBeginRequest beginRequest() {
        return beginRequest;
    }

    /**
     * Returns the {@code FCGI_PARAMS} record.
     * 
     * @return the {@code FcgiParams}
     */
    public FcgiParams params() {
        return params;
    }

    /**
     * Returns the {@code FCGI_STDIN} record.
     * 
     * @return the {@code FcgiStdin}
     */
    public FcgiStdin stdin() {
        return stdin;
    }

    /**
     * Returns the {@code FCGI_DATA} record if present.
     * 
     * @return an {@code Optional<FcgiData>}
     */
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
