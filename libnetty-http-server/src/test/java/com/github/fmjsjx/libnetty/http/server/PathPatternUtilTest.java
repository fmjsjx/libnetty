package com.github.fmjsjx.libnetty.http.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class PathPatternUtilTest {

    @Test
    public void testFromPathPattern() {
        try {
            PathPattern pp = PathPatternUtil.build("/users/{userId}/items/{propId}-{propType}/");
            assertNotNull(pp);
            Pattern p = pp.pattern();
            assertEquals("^/+users/+(?<userId>[^/]+)/+items/+(?<propId>[^/]+)\\-(?<propType>[^/]+)/*$", p.pattern());
            assertNotNull(pp.pathVariableNames());
            assertEquals(3, pp.pathVariableNames().size());
            assertIterableEquals(Arrays.asList("userId", "propId", "propType"), pp.pathVariableNames());
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
            m = p.matcher("/nusers/123/items/101-303/");
            assertFalse(m.matches());

            pp = PathPatternUtil.build("/users/{userId}/items/{propId}-{propType}");
            assertNotNull(pp);
            p = pp.pattern();
            assertEquals("^/+users/+(?<userId>[^/]+)/+items/+(?<propId>[^/]+)\\-(?<propType>[^/]+)/*$", p.pattern());
            assertNotNull(pp.pathVariableNames());
            assertEquals(3, pp.pathVariableNames().size());
            assertIterableEquals(Arrays.asList("userId", "propId", "propType"), pp.pathVariableNames());
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
            m = p.matcher("//users/123/items/101-303/");
            assertTrue(m.matches());
            assertEquals("123", m.group("userId"));
            assertEquals("101", m.group("propId"));
            assertEquals("303", m.group("propType"));
            m = p.matcher("//users//123/////items//101-303//");
            assertTrue(m.matches());
            assertEquals("123", m.group("userId"));
            assertEquals("101", m.group("propId"));
            assertEquals("303", m.group("propType"));
            m = p.matcher("/nusers/123/items/101-303/");
            assertFalse(m.matches());

            pp = PathPatternUtil.build("/images/{name}_{width}x{height}.{type}");
            assertNotNull(pp);
            p = pp.pattern();
            assertEquals("^/+images/+(?<name>[^/]+)_(?<width>[^/]+)x(?<height>[^/]+)\\.(?<type>[^/]+)/*$", p.pattern());
            assertNotNull(pp.pathVariableNames());
            assertEquals(4, pp.pathVariableNames().size());
            assertIterableEquals(Arrays.asList("name", "width", "height", "type"), pp.pathVariableNames());
            m = p.matcher("/images/test_1920x1080.png");
            assertTrue(m.matches());
            assertEquals("test", m.group("name"));
            assertEquals("1920", m.group("width"));
            assertEquals("1080", m.group("height"));
            assertEquals("png", m.group("type"));
            m = p.matcher("/images/error1920x1080.png");
            assertFalse(m.matches());
            m = p.matcher("/noimage/error_1920x1080.png");
            assertFalse(m.matches());

        } catch (Exception e) {
            fail(e);
        }

        try {
            PathPatternUtil.build("/error/{aa$$}/bb");
            fail("Should throws IllegalArgumentException but not!");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("illegal path variable {aa$$}", e.getLocalizedMessage());
        }
        try {
            PathPatternUtil.build("/error/{1aa}/bb");
            fail("Should throws IllegalArgumentException but not!");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("illegal path variable {1aa}", e.getLocalizedMessage());
        }
    }

}
