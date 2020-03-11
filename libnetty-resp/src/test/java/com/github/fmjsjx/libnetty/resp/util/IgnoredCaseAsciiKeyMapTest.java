package com.github.fmjsjx.libnetty.resp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import io.netty.util.AsciiString;

public class IgnoredCaseAsciiKeyMapTest {

    @Test
    public void testGet() {
        try {
            IgnoredCaseAsciiKeyMap<String> map = new IgnoredCaseAsciiKeyMap<>();
            map.put("test", "test");
            map.put("Hello", "hello");

            assertEquals("test", map.get(AsciiString.of("TEST")));
            assertEquals("test", map.get(AsciiString.of("test")));
            assertEquals("test", map.get(AsciiString.of("TesT")));

            assertEquals("hello", map.get(AsciiString.of("Hello")));
            assertEquals("hello", map.get(AsciiString.of("hello")));
            assertEquals("hello", map.get(AsciiString.of("HELLO")));
            assertEquals("hello", map.get(AsciiString.of("HellO")));
            assertEquals("hello", map.get(AsciiString.of("hELLo")));

            assertNull(map.get(AsciiString.of("none")));
        } catch (Exception e) {
            fail(e);
        }
    }

}
