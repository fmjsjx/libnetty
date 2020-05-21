package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Objects;

/**
 * The abstract implementation of {@link FcgiRecord}.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public abstract class AbstractFcgiRecord implements FcgiRecord {

    protected final FcgiVersion protocolVersion;
    protected final int requestId;

    /**
     * Constants a new instance.
     * 
     * @param protocolVersion the {@link FcgiVersion} of this record
     * @param requestId       the request id of this record
     */
    protected AbstractFcgiRecord(FcgiVersion protocolVersion, int requestId) {
        this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion must not be null");
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append('{').append(type().name()).append(", ").append(requestId())
                .append(", ");
        bodyToString(builder);
        return builder.append('}').toString();
    }

    protected void bodyToString(StringBuilder builder) {
        builder.append('"').append(bodyToString()).append('"');
    }

    protected String bodyToString() {
        return "";
    }

}
