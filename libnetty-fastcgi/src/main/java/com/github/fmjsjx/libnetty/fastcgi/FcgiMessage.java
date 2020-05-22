package com.github.fmjsjx.libnetty.fastcgi;

/**
 * An interface defines a FastCGI message.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public interface FcgiMessage extends FcgiObject {

    /**
     * Returns the {@link FcgiVersion}.
     * 
     * @return the {@code FcgiVersion}
     */
    FcgiVersion protocolVersion();

    /**
     * Returns the request id.
     * 
     * @return the request id
     */
    int requestId();

}
