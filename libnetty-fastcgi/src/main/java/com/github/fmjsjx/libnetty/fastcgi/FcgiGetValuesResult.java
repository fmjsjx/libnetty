package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A {@code FCGI_GET_VALUES_RESULT} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiGetValuesResult extends FcgiNameValuePairs<FcgiGetValuesResult> implements FcgiMessage {

    /**
     * Constructs a new {@link FcgiGetValuesResult} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     */
    public FcgiGetValuesResult(FcgiVersion protocolVersion) {
        super(protocolVersion, 0);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.GET_VALUES_RESULT;
    }

}
