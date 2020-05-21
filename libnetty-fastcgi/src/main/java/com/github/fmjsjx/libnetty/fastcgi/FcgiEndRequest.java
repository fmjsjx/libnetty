package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A {@code FCGI_END_REQUEST} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiEndRequest extends AbstractFcgiRecord {

    private final int appStatus;
    private final FcgiProtocolStatus protocolStatus;

    /**
     * Constructs a new {@link FcgiEndRequest} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param appStatus       the number of the {@code appStatus}
     * @param protocolStatus  the {@code FcgiProtocolStatus}
     */
    public FcgiEndRequest(FcgiVersion protocolVersion, int requestId, int appStatus,
            FcgiProtocolStatus protocolStatus) {
        super(protocolVersion, requestId);
        this.appStatus = appStatus;
        this.protocolStatus = protocolStatus;
    }

    /**
     * Constructs a new {@link FcgiEndRequest} instance with
     * {@code FCGI_REQUEST_COMPLETE}.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param appStatus       the number of the {@code appStatus}
     */
    public FcgiEndRequest(FcgiVersion protocolVersion, int requestId, int appStatus) {
        this(protocolVersion, requestId, appStatus, FcgiProtocolStatus.REQUEST_COMPLETE);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.END_REQUEST;
    }

    @Override
    public int contentLength() {
        return 5;
    }

    @Override
    public int paddingLength() {
        return 3;
    }

    /**
     * Returns the {@code appStatus} of this record.
     * 
     * @return the {@code appSatus}
     */
    public int appStatus() {
        return appStatus;
    }

    /**
     * Returns the {@link FcgiProtocolStatus} of this record.
     * 
     * @return a {@code FcgiProtocolStatus}
     */
    public FcgiProtocolStatus protocolStatus() {
        return protocolStatus;
    }

    @Override
    protected void bodyToString(StringBuilder builder) {
        builder.append('{').append(appStatus).append(", ").append(protocolStatus.name()).append('}');
    }

}
