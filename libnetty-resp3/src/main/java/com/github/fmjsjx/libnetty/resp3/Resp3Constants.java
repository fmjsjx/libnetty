package com.github.fmjsjx.libnetty.resp3;

/**
 * Constants of RESP3.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class Resp3Constants {
    
    /**
     * Sign of type null: {@code _}
     */
    public static final byte TYPE_NULL = '_';
    /**
     * Sign of type double: {@code ,}
     */
    public static final byte TYPE_DOUBLE = ',';
    /**
     * Sign of type boolean: {@code #}.
     */
    public static final byte TYPE_BOOLEAN = '#';
    /**
     * Sign of type blob error: {@code !}.
     */
    public static final byte TYPE_BLOB_ERROR = '!';
    /**
     * Sign of type verbatim string: {@code =}.
     */
    public static final byte TYPE_VERBATIM_STRING = '=';
    /**
     * Sign of type big number: {@code (}.
     */
    public static final byte TYPE_BIG_NUMBER = '(';
    /**
     * Sign of type map: {@code %}.
     */
    public static final byte TYPE_MAP = '%';
    /**
     * Sign of type set: {@code ~}.
     */
    public static final byte TYPE_SET = '~';
    /**
     * Sign of type attribute: {@code |}.
     */
    public static final byte TYPE_ATTRIBUTE = '|';
    /**
     * Sign of type push: {@code >}.
     */
    public static final byte TYPE_PUSH = '>';
    /**
     * Sign of type streamed string: {@code ;}.
     */
    public static final byte TYPE_STREAMED_STRING = ';';
    /**
     * Sign of type end: {@code .}.
     */
    public static final byte TYPE_END = '.';

}
