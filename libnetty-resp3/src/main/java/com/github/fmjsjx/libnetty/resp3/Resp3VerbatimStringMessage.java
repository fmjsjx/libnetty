package com.github.fmjsjx.libnetty.resp3;

import java.nio.charset.Charset;

import com.github.fmjsjx.libnetty.resp.RespContent;

import io.netty.util.CharsetUtil;

/**
 * An interface defines a RESP3 Verbatim String message.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3VerbatimStringMessage extends Resp3Message, RespContent {

    @Override
    default Resp3MessageType type() {
        return Resp3MessageType.VERBATIM_STRING;
    }

    /**
     * Returns the format part of this verbatim string.
     * 
     * @return the format part of this verbatim string
     */
    String formatPart();

    /**
     * Returns the {@link Format} of this verbatim string.
     * 
     * @return the {@link Format} of this verbatim string
     */
    Format format();

    /**
     * Returns the real string part as {@link String} type decoded with the specific
     * {@link Charset} given.
     * 
     * @param charset a {@code Charset}
     * @return the real string part
     */
    String textValue(Charset charset);

    /**
     * Returns the real string part as {@link String} type decoded with
     * {@code UTF-8} character set.
     * 
     * @return the real string part
     */
    default String textValue() {
        return textValue(CharsetUtil.UTF_8);
    }

    /**
     * Enumeration formats of Verbatim String.
     *
     * @author MJ Fang
     */
    public enum Format {

        /**
         * Plain text: {@code "txt"}
         */
        PLAIN_TEXT("txt"),
        /**
         * Markdown: {@code "mkd"}
         */
        MARKDOWN("mkd"),
        /**
         * Other: {@code "???"}
         */
        OTHER("");

        /**
         * Returns the {@link Format} from the specified abbreviation of the format
         * string.
         * 
         * @param abbr the abbreviation of the format string
         * @return a {@code Format}
         */
        public static final Format fromAbbr(String abbr) {
            switch (abbr) {
            case "txt":
                return PLAIN_TEXT;
            case "mkd":
                return MARKDOWN;
            default:
                return OTHER;
            }
        }

        private final String abbr;

        private Format(String abbr) {
            this.abbr = abbr;
        }

        @Override
        public String toString() {
            return name() + (abbr.isEmpty() ? "" : "(" + abbr + ")");
        }

    }

}
