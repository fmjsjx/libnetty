package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A FastCGI record.
 * 
 * @since 1.0
 *
 * @author fmjsjx
 */
public interface FcgiRecord extends FcgiObject {

    /**
     * Returns the FastCGI protocol version of this record.
     * 
     * @return the {@link FcgiVersion}
     */
    FcgiVersion protocolVersion();

    /**
     * Returns the FastCGI record type of this record.
     * 
     * @return the {@link FcgiRecordType}
     */
    FcgiRecordType type();

    /**
     * Returns the FastCGI request to which this record belongs.
     * 
     * @return the id of the request
     */
    int requestId();

    /**
     * Returns the number of bytes in the content data component of this record.
     * 
     * @return the length of the content data
     */
    int contentLength();

    /**
     * Returns the number of bytes in the padding data component of this record.
     * 
     * @return the length of the padding data
     */
    int paddingLength();

}
