package com.github.fmjsjx.libnetty.http2.server.util;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MimeTypesUtilTests {

    @Test
    public void testGetMimeType() {
        // JavaScript and TypeScript
        assertEquals(AsciiString.of("text/javascript"), MimeTypesUtil.getMimeType("test.js"));
        assertEquals(AsciiString.of("text/javascript"), MimeTypesUtil.getMimeType("test.mjs"));
        assertEquals(AsciiString.of("text/javascript"), MimeTypesUtil.getMimeType("test.ts"));

        // CSS
        assertEquals(AsciiString.of("text/css"), MimeTypesUtil.getMimeType("test.css"));

        // HTML
        assertEquals(AsciiString.of("text/html"), MimeTypesUtil.getMimeType("test.html"));
        assertEquals(AsciiString.of("text/html"), MimeTypesUtil.getMimeType("test.htm"));

        // JSON
        assertEquals(AsciiString.of("application/json"), MimeTypesUtil.getMimeType("test.json"));
        assertEquals(AsciiString.of("application/ld+json"), MimeTypesUtil.getMimeType("test.jsonld"));

        // XML
        assertEquals(AsciiString.of("application/xhtml+xml"), MimeTypesUtil.getMimeType("test.xhtml"));

        // Compression formats
        assertEquals(AsciiString.of("application/gzip"), MimeTypesUtil.getMimeType("test.gz"));
        assertEquals(AsciiString.of("application/zip"), MimeTypesUtil.getMimeType("test.zip"));
        assertEquals(AsciiString.of("application/x-bzip2"), MimeTypesUtil.getMimeType("test.bz2"));
        assertEquals(AsciiString.of("application/x-bzip"), MimeTypesUtil.getMimeType("test.bz"));
        assertEquals(AsciiString.of("application/x-7z-compressed"), MimeTypesUtil.getMimeType("test.7z"));
        assertEquals(AsciiString.of("application/vnd.rar"), MimeTypesUtil.getMimeType("test.rar"));
        assertEquals(AsciiString.of("application/x-tar"), MimeTypesUtil.getMimeType("test.tar"));

        // Image formats
        assertEquals(AsciiString.of("image/png"), MimeTypesUtil.getMimeType("test.png"));
        assertEquals(AsciiString.of("image/jpeg"), MimeTypesUtil.getMimeType("test.jpg"));
        assertEquals(AsciiString.of("image/jpeg"), MimeTypesUtil.getMimeType("test.jpeg"));
        assertEquals(AsciiString.of("image/gif"), MimeTypesUtil.getMimeType("test.gif"));
        assertEquals(AsciiString.of("image/svg+xml"), MimeTypesUtil.getMimeType("test.svg"));
        assertEquals(AsciiString.of("image/webp"), MimeTypesUtil.getMimeType("test.webp"));
        assertEquals(AsciiString.of("image/bmp"), MimeTypesUtil.getMimeType("test.bmp"));
        assertEquals(AsciiString.of("image/tiff"), MimeTypesUtil.getMimeType("test.tif"));
        assertEquals(AsciiString.of("image/x-icon"), MimeTypesUtil.getMimeType("test.ico"));

        // Audio formats
        assertEquals(AsciiString.of("audio/mpeg"), MimeTypesUtil.getMimeType("test.mp3"));
        assertEquals(AsciiString.of("audio/aac"), MimeTypesUtil.getMimeType("test.aac"));
        assertEquals(AsciiString.of("audio/wav"), MimeTypesUtil.getMimeType("test.wav"));
        assertEquals(AsciiString.of("audio/ogg"), MimeTypesUtil.getMimeType("test.ogg"));
        assertEquals(AsciiString.of("audio/flac"), MimeTypesUtil.getMimeType("test.flac"));

        // Video formats
        assertEquals(AsciiString.of("video/mp4"), MimeTypesUtil.getMimeType("test.mp4"));
        assertEquals(AsciiString.of("video/mpeg"), MimeTypesUtil.getMimeType("test.mpeg"));
        assertEquals(AsciiString.of("video/3gpp"), MimeTypesUtil.getMimeType("test.3gp"));
        assertEquals(AsciiString.of("video/quicktime"), MimeTypesUtil.getMimeType("test.mov"));
        assertEquals(AsciiString.of("video/webm"), MimeTypesUtil.getMimeType("test.webm"));
        assertEquals(AsciiString.of("video/x-msvideo"), MimeTypesUtil.getMimeType("test.avi"));

        // Document formats
        assertEquals(AsciiString.of("application/pdf"), MimeTypesUtil.getMimeType("test.pdf"));
        assertEquals(AsciiString.of("application/msword"), MimeTypesUtil.getMimeType("test.doc"));
        assertEquals(AsciiString.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                MimeTypesUtil.getMimeType("test.docx"));
        assertEquals(AsciiString.of("application/vnd.ms-excel"), MimeTypesUtil.getMimeType("test.xls"));
        assertEquals(AsciiString.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                MimeTypesUtil.getMimeType("test.xlsx"));
        assertEquals(AsciiString.of("application/vnd.ms-powerpoint"), MimeTypesUtil.getMimeType("test.ppt"));
        assertEquals(AsciiString.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                MimeTypesUtil.getMimeType("test.pptx"));

        // Text formats
        assertEquals(AsciiString.of("text/plain"), MimeTypesUtil.getMimeType("test.txt"));
        assertEquals(AsciiString.of("text/markdown"), MimeTypesUtil.getMimeType("test.md"));
        assertEquals(AsciiString.of("text/csv"), MimeTypesUtil.getMimeType("test.csv"));

        // Font formats
        assertEquals(AsciiString.of("font/ttf"), MimeTypesUtil.getMimeType("test.ttf"));
        assertEquals(AsciiString.of("font/otf"), MimeTypesUtil.getMimeType("test.otf"));
        assertEquals(AsciiString.of("font/woff"), MimeTypesUtil.getMimeType("test.woff"));
        assertEquals(AsciiString.of("font/woff2"), MimeTypesUtil.getMimeType("test.woff2"));

        // Archive formats
        assertEquals(AsciiString.of("application/java-archive"), MimeTypesUtil.getMimeType("test.jar"));
    }

    @Test
    public void testGetMimeTypeWithoutExtension() {
        assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM,
                MimeTypesUtil.getMimeType("test"));
    }

    @Test
    public void testGetMimeTypeWithDotOnly() {
        assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM,
                MimeTypesUtil.getMimeType("test."));
    }

    @Test
    public void testGetMimeTypeWithUnknownExtension() {
        assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM,
                MimeTypesUtil.getMimeType("test.unknown"));
    }

    @Test
    public void testGetMimeTypeCaseInsensitive() {
        assertEquals(AsciiString.of("text/javascript"), MimeTypesUtil.getMimeType("test.JS"));
        assertEquals(AsciiString.of("image/png"), MimeTypesUtil.getMimeType("test.PNG"));
        assertEquals(AsciiString.of("text/html"), MimeTypesUtil.getMimeType("test.HTML"));
    }

    @Test
    public void testGetMimeTypeWithPath() {
        assertEquals(AsciiString.of("text/javascript"),
                MimeTypesUtil.getMimeType("/path/to/test.js"));
        assertEquals(AsciiString.of("image/png"),
                MimeTypesUtil.getMimeType("path/to/test.png"));
    }

    @Test
    public void testGetMimeTypeWithMultipleDots() {
        assertEquals(AsciiString.of("application/gzip"),
                MimeTypesUtil.getMimeType("archive.tar.gz"));
        assertEquals(AsciiString.of("text/javascript"),
                MimeTypesUtil.getMimeType("my.component.test.js"));
    }

}
