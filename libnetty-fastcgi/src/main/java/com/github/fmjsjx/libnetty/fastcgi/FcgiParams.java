package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A {@code FCGI_PARAMS} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiParams extends FcgiNameValuePairs<FcgiParams> {

    /**
     * Constructs a new {@link FcgiParams} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     * @param requestId       the request id
     */
    public FcgiParams(FcgiVersion protocolVersion, int requestId) {
        super(protocolVersion, requestId);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.PARAMS;
    }

    @Override
    protected void bodyToString(StringBuilder builder) {
        builder.append('{');
        boolean hasValue = false;
        for (NameValuePair pair : pairs()) {
            if (hasValue) {
                builder.append(", ");
            } else {
                hasValue = true;
            }
            builder.append("(\"").append(pair.name().replace("\"", "\\\"")).append("\", \"")
                    .append(pair.value().replace("\"", "\\\"")).append("\")");
        }
        builder.append('}');
    }

}
