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

    /**
     * Returns this object as the specified type.
     * 
     * @param <T>  the type of this object to be cast
     * @param type the class of the type
     * @return this object
     */
    default <T extends FcgiRecord> T as(Class<T> type) {
        return type.cast(this);
    }

    /**
     * Returns this object as the specified type.
     * 
     * @param <T> the type of this object to be cast
     * @return this object
     */
    @SuppressWarnings("unchecked")
    default <T extends FcgiRecord> T as() {
        return (T) this;
    }

}
