package com.github.fmjsjx.libnetty.http.server.middleware;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class PathFilterMiddlewareTest {

    @Test
    public void testToPattern() {
        try {
            Pattern p = PathFilterMiddleware.toPattern("/api/test");
            assertNotNull(p);
            assertEquals("^/+api/+test(/+.*)?$", p.pattern());
            
            p = PathFilterMiddleware.toPattern("/api/test/");
            assertNotNull(p);
            assertEquals("^/+api/+test(/+.*)?$", p.pattern());
            
            p = PathFilterMiddleware.toPattern("//api//test//");
            assertNotNull(p);
            assertEquals("^/+api/+test(/+.*)?$", p.pattern());
        } catch (Exception e) {
            fail(e);
        }
    }
    
}
