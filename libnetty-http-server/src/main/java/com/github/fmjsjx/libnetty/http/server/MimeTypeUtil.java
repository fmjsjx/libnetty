package com.github.fmjsjx.libnetty.http.server;

import java.nio.file.Path;

import com.github.fmjsjx.libnetty.http.server.util.MimeTypesUtil;

/**
 * Utility class for mime type.
 *
 * @author MJ Fang
 * @since 1.1
 * @deprecated since 4.0, please use {@link MimeTypesUtil} instead.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated(since = "4.1")
public class MimeTypeUtil {

    /**
     * Probes the content-type of a file.
     *
     * @param path the {@link Path} of the file
     * @return the content type of the file
     * @deprecated since 4.1, please use
     * {@link MimeTypesUtil#probeContentType(Path)} instead.
     */
    @Deprecated(since = "4.1")
    public static final CharSequence probeContentType(Path path) {
        String filename = path.getFileName().toString();
        return MimeTypesUtil.getMimeType(filename);
    }

    private MimeTypeUtil() {
    }

}
