package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Optional;

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

    private static final Optional<FcgiProtocolStatus> R0 = Optional.of(REQUEST_COMPLETE);
    private static final Optional<FcgiProtocolStatus> R1 = Optional.of(CANT_MPX_CONN);
    private static final Optional<FcgiProtocolStatus> R2 = Optional.of(OVERLOADED);
    private static final Optional<FcgiProtocolStatus> R3 = Optional.of(UNKNOWN_ROLE);

    /**
     * Returns the {@link FcgiProtocolStatus} instance representing the specified
     * {@code status}.
     * 
     * @param status the number of the status
     * @return an {@code Optional<FcgiProtocolStatus>}
     */
    public static final Optional<FcgiProtocolStatus> valueOf(int status) {
        if (status == 1) {
            return R0;
        } else if (status == 2) {
            return R1;
        } else if (status == 3) {
            return R2;
        } else if (status == 4) {
            return R3;
        }
        return Optional.empty();
    }

    private final String name;
    private final int status;

    FcgiProtocolStatus(String name, int status) {
        this.name = name;
        this.status = status;
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

    @Override
    public String toString() {
        return "FcgiProtocolStatus(" + name + ", " + status + ")";
    }

}
