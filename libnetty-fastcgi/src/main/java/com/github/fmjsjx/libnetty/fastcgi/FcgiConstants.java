package com.github.fmjsjx.libnetty.fastcgi;

/**
 * In-package accessible constants of FastCGI.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
final class FcgiConstants {

    /**
     * Number of public static final bytes in a {@code FCGI_HEADER}. Future versions
     * of the protocol will not reduce this number.
     */
    static final int FCGI_HEADER_LEN = 8;

    /**
     * Maximum length of content data.
     */
    static final int FCGI_MAX_CONTENT_LENGTH = 65535;

    /**
     * Request id limit for complex services.
     */
    static final int FCGI_REQUEST_ID_LIMIT = 65536;

    /**
     * {@code FCGI_BEGIN_REQUEST 1}
     */
    static final byte FCGI_BEGIN_REQUEST = 1;
    /**
     * {@code FCGI_ABORT_REQUEST 2}
     */
    static final byte FCGI_ABORT_REQUEST = 2;
    /**
     * {@code FCGI_END_REQUEST 3}
     */
    static final byte FCGI_END_REQUEST = 3;
    /**
     * {@code FCGI_PARAMS 4}
     */
    static final byte FCGI_PARAMS = 4;
    /**
     * {@code FCGI_STDIN 5}
     */
    static final byte FCGI_STDIN = 5;
    /**
     * {@code FCGI_STDOUT 6}
     */
    static final byte FCGI_STDOUT = 6;
    /**
     * {@code FCGI_STDERR 7}
     */
    static final byte FCGI_STDERR = 7;
    /**
     * {@code FCGI_DATA 8}
     */
    static final byte FCGI_DATA = 8;
    /**
     * {@code FCGI_GET_VALUES 9}
     */
    static final byte FCGI_GET_VALUES = 9;
    /**
     * {@code FCGI_GET_VALUES_RESULT 10}
     */
    static final byte FCGI_GET_VALUES_RESULT = 10;
    /**
     * {@code FCGI_UNKNOWN_TYPE 11}
     */
    static final byte FCGI_UNKNOWN_TYPE = 11;

    /**
     * Mask for flags component of FCGI_BeginRequestBody
     */
    static final byte FCGI_KEEP_CONN = 1;

    /**
     * Empty value bytes.
     */
    static final byte[] EMPTY_VALUE = new byte[0];

    private FcgiConstants() {
    }

}
