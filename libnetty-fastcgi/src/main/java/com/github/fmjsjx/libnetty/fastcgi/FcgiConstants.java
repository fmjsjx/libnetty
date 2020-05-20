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
     * Mask for flags component of FCGI_BeginRequestBody
     */
    static final byte FCGI_KEEP_CONN = 1;

    private FcgiConstants() {
    }

}
