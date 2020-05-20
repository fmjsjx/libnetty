package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Optional;

/**
 * A FastCGI Role.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiRole {

    /**
     * {@code FCGI_RESPONDER 1}
     */
    public static final FcgiRole RESPONDER = new FcgiRole("RESPONDER", 1);
    /**
     * {@code FCGI_AUTHORIZER 2}
     */
    public static final FcgiRole AUTHORIZER = new FcgiRole("AUTHORIZER", 2);
    /**
     * {@code FCGI_FILTER 3}
     */
    public static final FcgiRole FILTER = new FcgiRole("FILTER", 3);

    private static final Optional<FcgiRole> R1 = Optional.of(RESPONDER);
    private static final Optional<FcgiRole> R2 = Optional.of(AUTHORIZER);
    private static final Optional<FcgiRole> R3 = Optional.of(FILTER);

    /**
     * Returns the {@link FcgiRole} instance representing the specified role.
     * 
     * @param role the number of the role
     * @return a {@code FcgiRole}
     */
    public static final Optional<FcgiRole> valueOf(int role) {
        if (role == 1) {
            return R1;
        } else if (role == 2) {
            return R2;
        } else if (role == 3) {
            return R3;
        }
        return Optional.empty();
    }

    private final String name;
    private final int role;

    FcgiRole(String name, int role) {
        this.name = name;
        this.role = role;
    }

    /**
     * Returns the name of this role.
     * 
     * @return the name of this role
     */
    public String name() {
        return name;
    }

    /**
     * Returns the number of this role.
     * 
     * @return the number of this role
     */
    public int role() {
        return role;
    }

    @Override
    public String toString() {
        return "FcgiRole(" + name + "," + role + ")";
    }

}
