package com.github.fmjsjx.libnetty.fastcgi;

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
    public static final FcgiRecordType BEGIN_REQUEST = new FcgiRecordType("BEGIN_REQUEST", 1);
    /**
     * {@code FCGI_ABORT_REQUEST 2}
     */
    public static final FcgiRecordType ABORT_REQUEST = new FcgiRecordType("ABORT_REQUEST", 2);
    /**
     * {@code FCGI_END_REQUEST 3}
     */
    public static final FcgiRecordType END_REQUEST = new FcgiRecordType("END_REQUEST", 3);
    /**
     * {@code FCGI_PARAMS 4}
     */
    public static final FcgiRecordType PARAMS = new FcgiRecordType("PARAMS", 4);
    /**
     * {@code FCGI_STDIN 5}
     */
    public static final FcgiRecordType STDIN = new FcgiRecordType("STDIN", 5);
    /**
     * {@code FCGI_STDOUT 6}
     */
    public static final FcgiRecordType STDOUT = new FcgiRecordType("STDOUT", 6);
    /**
     * {@code FCGI_STDERR 7}
     */
    public static final FcgiRecordType STDERR = new FcgiRecordType("STDERR", 7);
    /**
     * {@code FCGI_DATA 8}
     */
    public static final FcgiRecordType DATA = new FcgiRecordType("DATA", 8);
    /**
     * {@code FCGI_GET_VALUES 9}
     */
    public static final FcgiRecordType GET_VALUES = new FcgiRecordType("GET_VALUES", 9);
    /**
     * {@code FCGI_GET_VALUES_RESULT 10}
     */
    public static final FcgiRecordType GET_VALUES_RESULT = new FcgiRecordType("GET_VALUES_RESULT", 10);

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
        return new FcgiRecordType("UNKNOWN_TYPE", type);
    }

    private final String name;
    private final int type;

    FcgiRecordType(String name, int type) {
        this.name = name;
        this.type = type;
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
