package com.github.fmjsjx.libnetty.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class HttpUtilTest {

    @Test
    public void testContentType() {
        AsciiString ct1 = HttpUtil.contentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", ct1.toString());
        AsciiString ct2 = HttpUtil.contentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        assertEquals(ct1, ct2);
        assertTrue(ct1 == ct2);
        AsciiString ct3 = HttpUtil.contentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED, CharsetUtil.UTF_8);
        assertEquals(ct1, ct3);
        assertTrue(ct1 == ct3);
    }

}
