package com.github.fmjsjx.libnetty.http.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.netty.util.AsciiString;

public class MimeTypeUtilTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testProbeContentType() {
        assertEquals(AsciiString.of("text/javascript"), MimeTypeUtil.probeContentType(Paths.get("test.js")));
        assertEquals(AsciiString.of("text/css"), MimeTypeUtil.probeContentType(Paths.get("test.css")));
        assertEquals(AsciiString.of("text/html"), MimeTypeUtil.probeContentType(Paths.get("test.html")));
        assertEquals(AsciiString.of("application/json"), MimeTypeUtil.probeContentType(Paths.get("test.json")));
        assertEquals(AsciiString.of("application/xml"), MimeTypeUtil.probeContentType(Paths.get("test.xml")));
        assertEquals(AsciiString.of("application/gzip"), MimeTypeUtil.probeContentType(Paths.get("test.gz")));
        assertEquals(AsciiString.of("application/zip"), MimeTypeUtil.probeContentType(Paths.get("test.zip")));
        assertEquals(AsciiString.of("application/x-bzip2"), MimeTypeUtil.probeContentType(Paths.get("test.bz2")));
        assertEquals(AsciiString.of("image/png"), MimeTypeUtil.probeContentType(Paths.get("test.png")));
        assertEquals(AsciiString.of("image/jpeg"), MimeTypeUtil.probeContentType(Paths.get("test.jpg")));
        assertEquals(AsciiString.of("image/gif"), MimeTypeUtil.probeContentType(Paths.get("test.gif")));
        assertEquals(AsciiString.of("image/svg+xml"), MimeTypeUtil.probeContentType(Paths.get("test.svg")));
        assertEquals(AsciiString.of("audio/mpeg"), MimeTypeUtil.probeContentType(Paths.get("test.mp3")));
        assertEquals(AsciiString.of("video/mp4"), MimeTypeUtil.probeContentType(Paths.get("test.mp4")));
        assertEquals(AsciiString.of("video/3gpp"), MimeTypeUtil.probeContentType(Paths.get("test.3gp")));
        assertEquals(AsciiString.of("video/quicktime"), MimeTypeUtil.probeContentType(Paths.get("test.mov")));
    }

}
