package com.github.fmjsjx.libnetty.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class HttpCommonUtilTest {

    @Test
    public void testContentType() {
        AsciiString ct1 = HttpCommonUtil.contentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", ct1.toString());
        AsciiString ct2 = HttpCommonUtil.contentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        assertEquals(ct1, ct2);
        assertTrue(ct1 == ct2);
        AsciiString ct3 = HttpCommonUtil.contentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED,
                CharsetUtil.UTF_8);
        assertEquals(ct1, ct3);
        assertTrue(ct1 == ct3);
    }

    @Test
    public void testBasicAuthentication() {
        CharSequence auth = HttpCommonUtil.basicAuthentication("root", "123456");
        assertEquals("Basic " + Base64.getEncoder().encodeToString("root:123456".getBytes()), auth.toString());
        
        auth = HttpCommonUtil.basicAuthentication("test", "abc789");
        assertEquals("Basic " + Base64.getEncoder().encodeToString("test:abc789".getBytes()), auth.toString());
        
        auth = HttpCommonUtil.basicAuthentication("longpwd", "AbcdefgHijklmn1~3$5^7*9)");
        assertEquals("Basic " + Base64.getEncoder().encodeToString("longpwd:AbcdefgHijklmn1~3$5^7*9)".getBytes()), auth.toString());
    }

}
