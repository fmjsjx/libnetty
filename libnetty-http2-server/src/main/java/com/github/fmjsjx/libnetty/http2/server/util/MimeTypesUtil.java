package com.github.fmjsjx.libnetty.http2.server.util;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility class for mime types.
 *
 * @author MJ Fang
 * @since 4.1
 */
public class MimeTypesUtil {

    private static final Logger logger = LoggerFactory.getLogger(MimeTypesUtil.class);

    private static final AsciiString DEFAULT_MIME_TYPE = HttpHeaderValues.APPLICATION_OCTET_STREAM;

    private static final Map<String, MimeType> MIME_TYPES;

    static {
        var mimeTypesFiles = List.of(
                "/META-INF/mime-types.default",
                "/META-INF/mime-types",
                "/mime-types");
        var mimeTypes = new HashMap<String, MimeType>();
        var fill = MimeType.fillInto(mimeTypes);
        mimeTypesFiles.stream().flatMap(MimeType::loadFromResource).forEach(fill);
        MIME_TYPES = Map.copyOf(mimeTypes);
    }

    /**
     * Returns the MIME type based on the specified file name.
     *
     * @param filename the file name
     * @return the file's MIME type
     */
    public static AsciiString getMimeType(String filename) {
        var dotIndex = filename.lastIndexOf(".");
        if (dotIndex < 0) {
            return DEFAULT_MIME_TYPE;
        }
        var ext = filename.substring(dotIndex + 1);
        if (ext.isEmpty()) {
            return DEFAULT_MIME_TYPE;
        }
        var mimeType = MIME_TYPES.get(ext.toLowerCase());
        return mimeType != null ? mimeType.typeString() : DEFAULT_MIME_TYPE;
    }

    private MimeTypesUtil() {
    }

    private record MimeType(AsciiString typeString, List<String> extensions) {

        private static MimeType parse(String line) {
            var sharpIndex = line.indexOf('#');
            var text = (sharpIndex >= 0 ? line.substring(0, sharpIndex) : line).trim();
            if (text.isEmpty()) {
                return null;
            }
            var split = text.split("\\s+");
            if (split.length < 2) {
                // Invalid mime type string, skip it.
                return null;
            }
            return new MimeType(new AsciiString(split[0]), List.of(Arrays.copyOfRange(split, 1, split.length)));
        }

        private static Stream<MimeType> loadFromResource(String resourceName) {
            try (var in = MimeTypesUtil.class.getResourceAsStream(resourceName)) {
                if (in != null) {
                    try (var reader = new BufferedReader(new InputStreamReader(in))) {
                        return reader.lines().map(MimeType::parse).filter(Objects::nonNull).toList().stream();
                    }
                }
            } catch (IOException e) {
                logger.warn("Load mime types from resource {} failed.", resourceName, e);
            }
            return Stream.empty();
        }

        private static Consumer<MimeType> fillInto(Map<String, MimeType> mimeTypes) {
            return it -> it.extensions.forEach(ext -> mimeTypes.put(ext, it));
        }

    }

}
