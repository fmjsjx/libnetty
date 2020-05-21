package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A {@code FCGI_ABORT_REQUEST} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiAbortRequest extends AbstractFcgiRecord {

    /**
     * Constructs a new {@link FcgiAbortRequest} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     */
    public FcgiAbortRequest(FcgiVersion protocolVersion, int requestId) {
        super(protocolVersion, requestId);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.ABORT_REQUEST;
    }

    @Override
    public int contentLength() {
        return 0;
    }

    @Override
    public int paddingLength() {
        return 0;
    }

    @Override
    public String toString() {
        return "{FCGI_ABORT_REQUEST, " + requestId + "}";
    }

}
