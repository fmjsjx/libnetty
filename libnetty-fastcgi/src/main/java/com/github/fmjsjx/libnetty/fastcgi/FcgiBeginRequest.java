package com.github.fmjsjx.libnetty.fastcgi;

public class FcgiBeginRequest extends AbstractFcgiRecord {

    /**
     * {@code FCGI_KEEP_CONN 1}
     */
    public static final int KEEP_CONN = 1;

    private final FcgiRole role;
    private int flags;

    public FcgiBeginRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role) {
        this(protocolVersion, requestId, role, 0);
    }

    public FcgiBeginRequest(FcgiVersion protocolVersion, int requestId, FcgiRole role, int flags) {
        super(protocolVersion, requestId);
        this.role = role;
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

    public boolean isKeepConn() {
        return (flags & KEEP_CONN) != 0;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
