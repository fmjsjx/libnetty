package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_BULK_STRING;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_ATTRIBUTE;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_BIG_NUMBER;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_BLOB_ERROR;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_BOOLEAN;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_DOUBLE;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_END;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_MAP;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_NULL;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_PUSH;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_SET;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_STREAMED_STRING_PART;
import static com.github.fmjsjx.libnetty.resp3.Resp3Constants.TYPE_VERBATIM_STRING;

import java.util.NoSuchElementException;

import com.github.fmjsjx.libnetty.resp.RespMessageType;

/**
 * The message type of RESP3.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class Resp3MessageType extends RespMessageType {

    /**
     * RESP3 {@code Null}.
     */
    public static final Resp3MessageType NULL = new Resp3MessageType(TYPE_NULL, true, "Null");
    /**
     * RESP3 {@code Double}.
     */
    public static final Resp3MessageType DOUBLE = new Resp3MessageType(TYPE_DOUBLE, true, "Double");
    /**
     * RESP3 {@code Boolean}.
     */
    public static final Resp3MessageType BOOLEAN = new Resp3MessageType(TYPE_BOOLEAN, true, "Boolean");
    /**
     * RESP3 {@code BlobError}.
     */
    public static final Resp3MessageType BLOB_ERROR = new Resp3MessageType(TYPE_BLOB_ERROR, false, "BlobError");
    /**
     * RESP3 {@code VerbatimString}.
     */
    public static final Resp3MessageType VERBATIM_STRING = new Resp3MessageType(TYPE_VERBATIM_STRING, false,
            "VerbatimString");
    /**
     * RESP3 {@code BigNumber}.
     */
    public static final Resp3MessageType BIG_NUMBER = new Resp3MessageType(TYPE_BIG_NUMBER, true, "BigNumber");
    /**
     * RESP3 {@code Map}.
     */
    public static final Resp3MessageType MAP = new Resp3MessageType(TYPE_MAP, false, "Map");
    /**
     * RESP3 {@code Set}.
     */
    public static final Resp3MessageType SET = new Resp3MessageType(TYPE_SET, false, "Set");
    /**
     * RESP3 {@code Attribute}.
     */
    public static final Resp3MessageType ATTRIBUTE = new Resp3MessageType(TYPE_ATTRIBUTE, false, "Attribute");
    /**
     * RESP3 {@code Push}.
     */
    public static final Resp3MessageType PUSH = new Resp3MessageType(TYPE_PUSH, false, "Push");
    /**
     * RESP3 {@code StreamedStringHeader}.
     */
    public static final Resp3MessageType STREAMED_STRING_HEADER = new Resp3MessageType(TYPE_BULK_STRING, false,
            "StreamedStringHeader");
    /**
     * RESP3 {@code StreamedString}.
     */
    public static final Resp3MessageType STREAMED_STRING_PART = new Resp3MessageType(TYPE_STREAMED_STRING_PART, false,
            "StreamedStringPart");
    /**
     * RESP3 {@code End}.
     */
    public static final Resp3MessageType END = new Resp3MessageType(TYPE_END, true, "End");

    /**
     * Returns a {@link Resp3MessageType} instance by the sign {@code value} given.
     * 
     * @param value the sign value of the type
     * @return a {@code Resp3MessageType}
     */
    public static final Resp3MessageType fromSign(char value) {
        return fromSign((byte) value);
    }

    /**
     * Returns a {@link Resp3MessageType} instance by the sign {@code value} given.
     * 
     * @param value the sign value of the type
     * @return a {@code Resp3MessageType}
     */
    public static final Resp3MessageType fromSign(byte value) {
        switch (value) {
        case TYPE_NULL:
            return NULL;
        case TYPE_DOUBLE:
            return DOUBLE;
        case TYPE_BOOLEAN:
            return BOOLEAN;
        case TYPE_BLOB_ERROR:
            return BLOB_ERROR;
        case TYPE_VERBATIM_STRING:
            return VERBATIM_STRING;
        case TYPE_BIG_NUMBER:
            return BIG_NUMBER;
        case TYPE_MAP:
            return MAP;
        case TYPE_SET:
            return SET;
        case TYPE_ATTRIBUTE:
            return ATTRIBUTE;
        case TYPE_PUSH:
            return PUSH;
        case TYPE_STREAMED_STRING_PART:
            return STREAMED_STRING_PART;
        case TYPE_END:
            return END;
        default:
            throw new NoSuchElementException("Unknown Sign '" + value + "'");
        }
    }

    private Resp3MessageType(byte signChar, boolean inline, String name) {
        super(signChar, inline, name);
    }

}
