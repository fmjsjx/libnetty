package com.github.fmjsjx.libnetty.fastcgi;

import static com.github.fmjsjx.libnetty.fastcgi.FcgiConstants.*;

import java.util.stream.Stream;

/**
 * The type of FastCGI records.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiRecordType {

    /**
     * {@code FCGI_BEGIN_REQUEST 1}
     */
    public static final FcgiRecordType BEGIN_REQUEST = new FcgiRecordType("BEGIN_REQUEST", FCGI_BEGIN_REQUEST);
    /**
     * {@code FCGI_ABORT_REQUEST 2}
     */
    public static final FcgiRecordType ABORT_REQUEST = new FcgiRecordType("ABORT_REQUEST", FCGI_ABORT_REQUEST);
    /**
     * {@code FCGI_END_REQUEST 3}
     */
    public static final FcgiRecordType END_REQUEST = new FcgiRecordType("END_REQUEST", FCGI_END_REQUEST);
    /**
     * {@code FCGI_PARAMS 4}
     */
    public static final FcgiRecordType PARAMS = new FcgiRecordType("PARAMS", FCGI_PARAMS);
    /**
     * {@code FCGI_STDIN 5}
     */
    public static final FcgiRecordType STDIN = new FcgiRecordType("STDIN", FCGI_STDIN);
    /**
     * {@code FCGI_STDOUT 6}
     */
    public static final FcgiRecordType STDOUT = new FcgiRecordType("STDOUT", FCGI_STDOUT);
    /**
     * {@code FCGI_STDERR 7}
     */
    public static final FcgiRecordType STDERR = new FcgiRecordType("STDERR", FCGI_STDERR);
    /**
     * {@code FCGI_DATA 8}
     */
    public static final FcgiRecordType DATA = new FcgiRecordType("DATA", FCGI_DATA);
    /**
     * {@code FCGI_GET_VALUES 9}
     */
    public static final FcgiRecordType GET_VALUES = new FcgiRecordType("GET_VALUES", FCGI_GET_VALUES);
    /**
     * {@code FCGI_GET_VALUES_RESULT 10}
     */
    public static final FcgiRecordType GET_VALUES_RESULT = new FcgiRecordType("GET_VALUES_RESULT",
            FCGI_GET_VALUES_RESULT);
    /**
     * {@code FCGI_UNKNOWN_TYPE 11}
     */
    public static final FcgiRecordType UNKNOWN_TYPE = new FcgiRecordType("UNKNOWN_TYPE", FCGI_UNKNOWN_TYPE);

    private static final FcgiRecordType[] values;

    static {
        values = Stream.of(null, BEGIN_REQUEST, ABORT_REQUEST, END_REQUEST, PARAMS, STDIN, STDOUT, STDERR, DATA,
                GET_VALUES, GET_VALUES_RESULT).toArray(FcgiRecordType[]::new);
    }

    /**
     * Returns the {@link FcgiRecordType} instance representing the specified type.
     * 
     * @param type the number of type
     * @return a {@code FcgiRecordType}
     */
    public static final FcgiRecordType valueOf(int type) {
        if (type > 0 && type < values.length) {
            return values[type];
        }
        return new FcgiRecordType("UNKNOWN_TYPE", type, true);
    }

    private final String name;
    private final int type;
    private final boolean unknown;

    FcgiRecordType(String name, int type) {
        this(name, type, false);
    }

    FcgiRecordType(String name, int type, boolean unknown) {
        this.name = name;
        this.type = type;
        this.unknown = unknown;
    }

    /**
     * Returns the number of this type.
     * 
     * @return the number of this type
     */
    public int type() {
        return type;
    }

    /**
     * Returns the name of this type.
     * 
     * @return the name of this type
     */
    public String name() {
        return name;
    }

    /**
     * Returns {@code true} if is type is unknown.
     * 
     * @return {@code true} if unknown
     */
    public boolean isUnknown() {
        return unknown;
    }

    @Override
    public int hashCode() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FcgiRecordType) {
            return ((FcgiRecordType) obj).type == type;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FcgiRecordType(" + name + "," + type + ")";
    }

}
