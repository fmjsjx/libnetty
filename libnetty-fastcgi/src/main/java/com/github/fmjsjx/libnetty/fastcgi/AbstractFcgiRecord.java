package com.github.fmjsjx.libnetty.fastcgi;

/**
 * The abstract implementation of {@link FcgiRecord}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class AbstractFcgiRecord implements FcgiRecord {

    private final FcgiVersion protocolVersion;
    private final int requestId;

    /**
     * Constants a new instance.
     * 
     * @param protocolVersion the {@link FcgiVersion} of this record
     * @param requestId       the request id of this record
     */
    protected AbstractFcgiRecord(FcgiVersion protocolVersion, int requestId) {
        this.protocolVersion = protocolVersion;
        this.requestId = requestId;
    }

    @Override
    public FcgiVersion protocolVersion() {
        return protocolVersion;
    }

    @Override
    public int requestId() {
        return requestId;
    }

}
