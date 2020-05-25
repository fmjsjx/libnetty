package com.github.fmjsjx.libnetty.fastcgi;

/**
 * A FastCGI role.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiRole {

    /**
     * {@code FCGI_RESPONDER 1}
     */
    public static final FcgiRole RESPONDER = new FcgiRole("FCGI_RESPONDER", 1);
    /**
     * {@code FCGI_AUTHORIZER 2}
     */
    public static final FcgiRole AUTHORIZER = new FcgiRole("FCGI_AUTHORIZER", 2);
    /**
     * {@code FCGI_FILTER 3}
     */
    public static final FcgiRole FILTER = new FcgiRole("FCGI_FILTER", 3);

    /**
     * Returns the {@link FcgiRole} instance representing the specified role.
     * 
     * @param role the number of the role
     * @return a {@code FcgiRole}
     */
    public static final FcgiRole valueOf(int role) {
        if (role == 1) {
            return RESPONDER;
        } else if (role == 2) {
            return AUTHORIZER;
        } else if (role == 3) {
            return FILTER;
        }
        return new FcgiRole("UNKNOWN", role, true);
    }

    private final String name;
    private final int role;
    private final boolean unknown;

    FcgiRole(String name, int role) {
        this(name, role, false);
    }

    FcgiRole(String name, int role, boolean unknown) {
        this.name = name;
        this.role = role;
        this.unknown = unknown;
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

    /**
     * Returns {@code true} if this role is unknown.
     * 
     * @return {@code true} if this role is unknown
     */
    public boolean isUnknown() {
        return unknown;
    }

    @Override
    public String toString() {
        return "FcgiRole(" + name + ", " + role + ")";
    }

}
