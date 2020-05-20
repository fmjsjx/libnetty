package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Objects;

/**
 * A {@code FCGI_BEGIN_REQUEST} record.
 *
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiBeginRequest extends AbstractFcgiRecord {

    /**
     * {@code FCGI_KEEP_CONN 1}
     */
    public static final int KEEP_CONN = 1;

    private final FcgiRole role;
    private int flags;

    /**
     * Constructs a new {@link FcgiBeginRequest} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param role            the {@code FcgiRole}
     */
    public FcgiBeginRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role) {
        this(protocolVersion, requestId, role, 0);
    }

    /**
     * Constructs a new {@link FcgiBeginRequest} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param role            the {@code FcgiRole}
     * @param flags           the flags
     */
    public FcgiBeginRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role, int flags) {
        super(protocolVersion, requestId);
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.flags = flags;
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.BEGIN_REQUEST;
    }

    @Override
    public int contentLength() {
        return 3;
    }

    @Override
    public int paddingLength() {
        return 5;
    }

    /**
     * Returns the {@link FcgiRole} of this {@link FcgiBeginRequest}.
     * 
     * @return a {@code FcgiRole}
     */
    public FcgiRole role() {
        return role;
    }

    /**
     * Returns {@code true} if the flags of this {@link FcgiBeginRequest} contains
     * the specified {@code flag}.
     * 
     * @param flag the flag
     * @return {@code true} if the flags contains the specified {@code flag}
     */
    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }

    /**
     * Sets the flags of this {@link FcgiBeginRequest} be with the specified
     * {@code flag}.
     * 
     * @param flag the flag
     * @return this {@code FcgiBeginRequest}
     */
    public FcgiBeginRequest withFlag(int flag) {
        flags |= flag;
        return this;
    }

    /**
     * Sets the flags of this {@link FcgiBeginRequest} be without the specified
     * {@code flag}.
     * 
     * @param flag the flag
     * @return this {@code FcgiBeginRequest}
     */
    public FcgiBeginRequest withoutFlag(int flag) {
        flags &= ~flag;
        return this;
    }

    /**
     * Returns {@code true} if the flags contains {@code FCGI_KEEP_CONN}.
     * 
     * @return {@code true} if the flags contains {@code FCGI_KEEP_CONN}
     */
    public boolean isKeepConn() {
        return hasFlag(KEEP_CONN);
    }

    /**
     * Sets the flags be with {@code FCGI_KEEP_CONN}.
     * 
     * @return this {@code FcgiBeginRequest}
     */
    public FcgiBeginRequest keepConn() {
        return keepConn(true);
    }

    /**
     * Sets the flags be with or without {@code FCGI_KEEP_CONN}.
     * 
     * @param keepConn {@code true} if need be with {@code FCGI_KEEP_CONN}
     * @return this {@code FcgiBeginRequest}
     */
    public FcgiBeginRequest keepConn(boolean keepConn) {
        return keepConn ? withFlag(KEEP_CONN) : withoutFlag(KEEP_CONN);
    }

    @Override
    public String toString() {
        return "{FCGI_BEGIN_REQUEST, " + requestId() + ", {" + role.name() + ", " + flagsToString() + "}}";
    }

    private String flagsToString() {
        return isKeepConn() ? "FCGI_KEEP_CONN" : "0";
    }

}
