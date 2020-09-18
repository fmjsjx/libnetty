package com.github.fmjsjx.libnetty.http.server;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_XHTML;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_XML;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_CSS;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.netty.util.AsciiString;

/**
 * Utility class for mime type.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class MimeTypeUtil {

    private static final AsciiString DEFAULT_MIME_TYPE = APPLICATION_OCTET_STREAM;

    private static final Map<String, CharSequence> mimeTypes;

    static {
        Map<String, CharSequence> types = new HashMap<>(256);
        types.put("bin", APPLICATION_OCTET_STREAM);
        types.put("exe", APPLICATION_OCTET_STREAM);
        types.put("css", TEXT_CSS);
        types.put("html", TEXT_HTML);
        types.put("htm", TEXT_HTML);
        types.put("json", APPLICATION_JSON);
        types.put("txt", TEXT_PLAIN);
        types.put("xhtml", APPLICATION_XHTML);
        types.put("xml", APPLICATION_XML);
        Properties props = new Properties();
        try (InputStream in = MimeTypeUtil.class.getResourceAsStream("/mime-types.properties")) {
            props.load(in);
        } catch (IOException e) {
            // ignore
        }
        props.forEach((k, v) -> {
            types.put(k.toString().toLowerCase(), AsciiString.cached(v.toString()));
        });
        mimeTypes = types;
    }

    /**
     * Probes the content type of a file.
     * 
     * @param path the {@link Path} of the file
     * 
     * @return the content type of the file
     */
    public static final CharSequence probeContentType(Path path) {
        String filename = path.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            int extBegin = lastDot + 1;
            if (extBegin == filename.length()) {
                // dot is last char
                return DEFAULT_MIME_TYPE;
            }
            String extension = filename.substring(extBegin).toLowerCase();
            CharSequence type = mimeTypes.get(extension);
            if (type != null) {
                return type;
            }
        }
        return DEFAULT_MIME_TYPE;
    }

    private MimeTypeUtil() {
    }

}
