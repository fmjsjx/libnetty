package com.github.fmjsjx.libnetty.http.client.util;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.util.AsciiString;
import org.junit.jupiter.api.Test;

import java.util.Base64;

public class HttpRequestUtilTests {

    @Test
    public void testAuthBasic() {
        var auth = HttpRequestUtil.authBasic("root", "Aa123456!", Base64.getEncoder());
        assertEquals(AsciiString.cached("Basic cm9vdDpBYTEyMzQ1NiE="), auth);
        auth = HttpRequestUtil.authBasic("root", null, Base64.getEncoder());
        assertEquals(AsciiString.cached("Basic cm9vdA=="), auth);
        auth = HttpRequestUtil.authBasic(null, "Aa123456!", Base64.getEncoder());
        assertEquals(AsciiString.cached("Basic OkFhMTIzNDU2IQ=="), auth);
        auth = HttpRequestUtil.authBasic(null, null, Base64.getEncoder());
        assertEquals(AsciiString.cached("Basic "), auth);
    }

}
