package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A {@code Fast-CGI} record.
 * 
 * @since 1.0
 *
 * @author fmjsjx
 */
public interface FastCGIRecord extends FastCGIObject {

    /**
     * Returns the FastCGI protocol version.
     * 
     * @return the version number
     */
    byte version();

    /**
     * Returns the type of this record.
     * 
     * @return the type number
     */
    byte type();

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
