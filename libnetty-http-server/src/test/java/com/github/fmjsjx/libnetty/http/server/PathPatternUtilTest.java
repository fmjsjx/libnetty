package com.github.fmjsjx.libnetty.http.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class PathPatternUtilTest {

    @Test
    public void testFromPathPattern() {
        try {
            Pattern p = PathPatternUtil.fromPathPattern("/users/{userId}/items/{propId}-{propType}/");
            assertNotNull(p);
            assertEquals("^/users/(?<userId>[^/]+)/items/(?<propId>[^/]+)\\-(?<propType>[^/]+)/?", p.pattern());
            Matcher m = p.matcher("/users/123/items/101-303");
            assertTrue(m.matches());
            assertEquals("123", m.group("userId"));
            assertEquals("101", m.group("propId"));
            assertEquals("303", m.group("propType"));
            m = p.matcher("/users/123/items/101-303/");
            assertTrue(m.matches());
            assertEquals("123", m.group("userId"));
            assertEquals("101", m.group("propId"));
            assertEquals("303", m.group("propType"));

            p = PathPatternUtil.fromPathPattern("/users/{userId}/items/{propId}-{propType}");
            assertNotNull(p);
            assertEquals("^/users/(?<userId>[^/]+)/items/(?<propId>[^/]+)\\-(?<propType>[^/]+)/?", p.pattern());
            m = p.matcher("/users/123/items/101-303");
            assertTrue(m.matches());
            assertEquals("123", m.group("userId"));
            assertEquals("101", m.group("propId"));
            assertEquals("303", m.group("propType"));
            m = p.matcher("/users/123/items/101-303/");
            assertTrue(m.matches());
            assertEquals("123", m.group("userId"));
            assertEquals("101", m.group("propId"));
            assertEquals("303", m.group("propType"));

            p = PathPatternUtil.fromPathPattern("/images/{name}_{width}x{height}.{type}");
            assertNotNull(p);
            assertEquals("^/images/(?<name>[^/]+)_(?<width>[^/]+)x(?<height>[^/]+)\\.(?<type>[^/]+)/?", p.pattern());
            m = p.matcher("/images/test_1920x1080.png");
            assertTrue(m.matches());
            assertEquals("test", m.group("name"));
            assertEquals("1920", m.group("width"));
            assertEquals("1080", m.group("height"));
            assertEquals("png", m.group("type"));
        } catch (Exception e) {
            fail(e);
        }
    }

}
