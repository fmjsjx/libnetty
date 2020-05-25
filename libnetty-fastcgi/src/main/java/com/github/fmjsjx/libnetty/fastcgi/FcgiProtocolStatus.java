package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A FastCGI protocol status.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiProtocolStatus {

    /**
     * {@code FCGI_REQUEST_COMPLETE 0}
     */
    public static final FcgiProtocolStatus REQUEST_COMPLETE = new FcgiProtocolStatus("FCGI_REQUEST_COMPLETE", 0);
    /**
     * {@code FCGI_CANT_MPX_CONN 1}
     */
    public static final FcgiProtocolStatus CANT_MPX_CONN = new FcgiProtocolStatus("FCGI_CANT_MPX_CONN", 1);
    /**
     * {@code FCGI_OVERLOADED 2}
     */
    public static final FcgiProtocolStatus OVERLOADED = new FcgiProtocolStatus("FCGI_OVERLOADED", 2);
    /**
     * {@code FCGI_UNKNOWN_ROLE 3}
     */
    public static final FcgiProtocolStatus UNKNOWN_ROLE = new FcgiProtocolStatus("FCGI_UNKNOWN_ROLE", 3);

    /**
     * Returns the {@link FcgiProtocolStatus} instance representing the specified
     * {@code status}.
     * 
     * @param status the number of the status
     * @return a {@code FcgiProtocolStatus}
     */
    public static final FcgiProtocolStatus valueOf(int status) {
        if (status == 0) {
            return REQUEST_COMPLETE;
        } else if (status == 1) {
            return CANT_MPX_CONN;
        } else if (status == 2) {
            return OVERLOADED;
        } else if (status == 3) {
            return UNKNOWN_ROLE;
        }
        return new FcgiProtocolStatus("UNKNOWN", status, true);
    }

    private final String name;
    private final int status;
    private final boolean unknown;

    FcgiProtocolStatus(String name, int status) {
        this(name, status, false);
    }

    FcgiProtocolStatus(String name, int status, boolean unknown) {
        this.name = name;
        this.status = status;
        this.unknown = unknown;
    }

    /**
     * Returns the name of this status.
     * 
     * @return the name of this status
     */
    public String name() {
        return name;
    }

    /**
     * Returns the number of this status.
     * 
     * @return the number of this status
     */
    public int status() {
        return status;
    }

    /**
     * Returns {@code true} if this status is unknown.
     * 
     * @return {@code true} if this status is unknown
     */
    public boolean isUnknown() {
        return unknown;
    }

    @Override
    public String toString() {
        return "FcgiProtocolStatus(" + name + ", " + status + ")";
    }

}
