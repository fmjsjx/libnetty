package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A {@code FCGI_UNKNOWN_TYPE} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiUnknownType extends AbstractFcgiRecord implements FcgiMessage {

    private final int type;

    /**
     * Constructs a new {@link FcgiUnknownType} instance with the specified
     * {@code type}.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     * @param type            number of the type
     */
    public FcgiUnknownType(FcgiVersion protocolVersion, int requestId, int type) {
        super(protocolVersion, requestId);
        this.type = type;
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.UNKNOWN_TYPE;
    }

    /**
     * Returns the number of the type.
     * 
     * @return the number of the type
     */
    public int value() {
        return type;
    }

    @Override
    public int contentLength() {
        return 1;
    }

    @Override
    public int paddingLength() {
        return 7;
    }

    @Override
    protected void bodyToString(StringBuilder builder) {
        builder.append('{').append(type).append('}');
    }

}
