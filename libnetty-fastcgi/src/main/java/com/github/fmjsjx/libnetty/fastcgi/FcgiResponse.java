package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Objects;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;

/**
 * A FastCGI response.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiResponse extends AbstractReferenceCounted implements FcgiMessage {

    private final FcgiVersion protocolVersion;
    private final int requestId;

    private final FcgiStdout stdout;
    private final Optional<FcgiStderr> stderr;
    private final FcgiEndRequest endRequest;

    FcgiResponse(FcgiEndRequest endRequest, FcgiStdout stdout, FcgiStderr stderr) {
        this.endRequest = Objects.requireNonNull(endRequest, "endRequest must not be null");
        this.stdout = Objects.requireNonNull(stdout, "stdout must not be null");
        this.stderr = Optional.ofNullable(stderr);
        this.protocolVersion = endRequest.protocolVersion();
        this.requestId = endRequest.requestId();
    }

    /**
     * Constructs a new {@link FcgiResponse} instance with protocolStatus
     * {@code FCGI_REQUEST_COMPLETE}.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param appStatus       the appStatus of {@code FCGI_END_REQUEST} record
     * @param stdoutContent   the {@code ByteBuf} content of {@code FCGI_STDOUT}
     */
    public FcgiResponse(FcgiVersion protocolVersion, int requestId, int appStatus, ByteBuf stdoutContent) {
        this(protocolVersion, requestId, appStatus, stdoutContent, null);
    }

    /**
     * Constructs a new {@link FcgiResponse} instance with protocolStatus
     * {@code FCGI_REQUEST_COMPLETE}.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param appStatus       the appStatus of {@code FCGI_END_REQUEST} record
     * @param stdoutContent   the {@code ByteBuf} content of {@code FCGI_STDOUT}
     * @param stderrContent   the {@code ByteBuf} content of {@code FCGI_STDERR}
     */
    public FcgiResponse(FcgiVersion protocolVersion, int requestId, int appStatus, ByteBuf stdoutContent,
            ByteBuf stderrContent) {
        this(protocolVersion, requestId, appStatus, FcgiProtocolStatus.REQUEST_COMPLETE, stdoutContent, stderrContent);
    }

    /**
     * Constructs a new {@link FcgiResponse} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param appStatus       the appStatus of {@code FCGI_END_REQUEST} record
     * @param protocolStatus  the protocolStatus of {@code FCGI_END_REQUEST} record
     * @param stdoutContent   the {@code ByteBuf} content of {@code FCGI_STDOUT}
     * @param stderrContent   the {@code ByteBuf} content of {@code FCGI_STDERR}
     */
    public FcgiResponse(FcgiVersion protocolVersion, int requestId, int appStatus, FcgiProtocolStatus protocolStatus,
            ByteBuf stdoutContent, ByteBuf stderrContent) {
        this.protocolVersion = protocolVersion;
        this.requestId = requestId;
        this.endRequest = new FcgiEndRequest(protocolVersion, requestId, appStatus, protocolStatus);
        this.stdout = new FcgiStdout(protocolVersion, requestId, stdoutContent);
        if (stderrContent != null) {
            this.stderr = Optional.of(new FcgiStderr(protocolVersion, requestId, stderrContent));
        } else {
            this.stderr = Optional.empty();
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
     * Returns the {@code FCGI_STDOUT} record.
     * 
     * @return the {@code FcgiStdout}
     */
    public FcgiStdout stdout() {
        return stdout;
    }

    /**
     * Returns the {@code FCGI_STDERR} record if present.
     * 
     * @return an {@code Optional<FcgiStderr>}
     */
    public Optional<FcgiStderr> stderr() {
        return stderr;
    }

    /**
     * Returns the {@code FCGI_END_REQUEST} record.
     * 
     * @return the {@code FcgiEndRequest}
     */
    public FcgiEndRequest endRequest() {
        return endRequest;
    }

    @Override
    public FcgiResponse retain() {
        super.retain();
        return this;
    }

    @Override
    public FcgiResponse retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public FcgiResponse touch() {
        super.touch();
        return this;
    }

    @Override
    public FcgiResponse touch(Object hint) {
        stdout.touch(hint);
        stderr.ifPresent(b -> b.touch(hint));
        return this;
    }

    @Override
    protected void deallocate() {
        stdout.release();
        stderr.ifPresent(FcgiStderr::release);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(stdout);
        stderr.ifPresent(b -> builder.append('\n').append(b));
        return builder.append('\n').append(endRequest).toString();
    }

}
