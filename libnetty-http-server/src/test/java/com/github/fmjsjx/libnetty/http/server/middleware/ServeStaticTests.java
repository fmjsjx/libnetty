package com.github.fmjsjx.libnetty.http.server.middleware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ServeStatic#parseRange(String)}.
 * <p>
 * The internal {@code Range} record is private to {@link ServeStatic}, so the
 * test code accesses its components via reflection.
 */
public class ServeStaticTests {

    /**
     * Helper that reflectively reads a component of the package-private
     * {@code ServeStatic.Range} record.
     */
    private static Object component(Object range, String name) {
        try {
            Method m = range.getClass().getDeclaredMethod(name);
            m.setAccessible(true);
            return m.invoke(range);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long start(Object range) {
        return (long) component(range, "start");
    }

    private static long end(Object range) {
        return (long) component(range, "end");
    }

    private static boolean valid(Object range) {
        return (boolean) component(range, "valid");
    }

    // ---------- 1. Normal range header formats ----------

    @Test
    public void testParseRange_explicit() {
        List<?> ranges = ServeStatic.parseRange("bytes=0-100");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(0L, start(r));
        assertEquals(100L, end(r));
        assertTrue(valid(r));
    }

    @Test
    public void testParseRange_openEnded() {
        List<?> ranges = ServeStatic.parseRange("bytes=100-");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(100L, start(r));
        assertEquals(-1L, end(r));
        assertTrue(valid(r));
    }

    @Test
    public void testParseRange_suffix() {
        List<?> ranges = ServeStatic.parseRange("bytes=-100");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(-100L, start(r));
        assertEquals(-1L, end(r));
        // For suffix-byte-range-spec, start <= end (-100 <= -1) so valid is true.
        assertTrue(valid(r));
    }

    @Test
    public void testParseRange_zeroExplicit() {
        List<?> ranges = ServeStatic.parseRange("bytes=0-0");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(0L, start(r));
        assertEquals(0L, end(r));
        assertTrue(valid(r));
    }

    @Test
    public void testParseRange_multipleRanges() {
        List<?> ranges = ServeStatic.parseRange("bytes=0-100,200-300");
        assertNotNull(ranges);
        assertEquals(2, ranges.size());

        Object r0 = ranges.getFirst();
        assertEquals(0L, start(r0));
        assertEquals(100L, end(r0));
        assertTrue(valid(r0));

        Object r1 = ranges.get(1);
        assertEquals(200L, start(r1));
        assertEquals(300L, end(r1));
        assertTrue(valid(r1));
    }

    @Test
    public void testParseRange_multipleRangesMixedTypes() {
        List<?> ranges = ServeStatic.parseRange("bytes=0-100,300-400,500-,-50");
        assertNotNull(ranges);
        assertEquals(4, ranges.size());

        assertEquals(0L, start(ranges.getFirst()));
        assertEquals(100L, end(ranges.getFirst()));
        assertTrue(valid(ranges.getFirst()));

        assertEquals(300L, start(ranges.get(1)));
        assertEquals(400L, end(ranges.get(1)));
        assertTrue(valid(ranges.get(1)));

        assertEquals(500L, start(ranges.get(2)));
        assertEquals(-1L, end(ranges.get(2)));
        assertTrue(valid(ranges.get(2)));

        assertEquals(-50L, start(ranges.get(3)));
        assertEquals(-1L, end(ranges.get(3)));
        assertTrue(valid(ranges.get(3)));
    }

    // ---------- 2. Boundary / invalid input ----------

    @Test
    public void testParseRange_emptyString() {
        assertNull(ServeStatic.parseRange(""));
    }

    @Test
    public void testParseRange_missingEquals() {
        assertNull(ServeStatic.parseRange("bytes"));
        assertNull(ServeStatic.parseRange("bytes 0-100"));
        assertNull(ServeStatic.parseRange("invalid-format"));
    }

    @Test
    public void testParseRange_emptyRangeSet() {
        assertNull(ServeStatic.parseRange("bytes="));
        assertNull(ServeStatic.parseRange("bytes=   "));
    }

    @Test
    public void testParseRange_unsupportedUnit() {
        assertNull(ServeStatic.parseRange("items=0-100"));
        assertNull(ServeStatic.parseRange("kilobytes=0-1"));
        assertNull(ServeStatic.parseRange("=0-100"));
    }

    @Test
    public void testParseRange_nonNumeric() {
        assertNull(ServeStatic.parseRange("bytes=abc"));
        assertNull(ServeStatic.parseRange("bytes=abc-def"));
        assertNull(ServeStatic.parseRange("bytes=0-abc"));
        assertNull(ServeStatic.parseRange("bytes=abc-100"));
        assertNull(ServeStatic.parseRange("bytes=1.5-2.5"));
    }

    @Test
    public void testParseRange_malformedDashes() {
        assertNull(ServeStatic.parseRange("bytes=--100"));
        assertNull(ServeStatic.parseRange("bytes=0-1-2"));
        assertNull(ServeStatic.parseRange("bytes=100"));     // no dash at all
        assertNull(ServeStatic.parseRange("bytes=-"));       // both sides empty
    }

    @Test
    public void testParseRange_emptySegmentInRangeSet() {
        assertNull(ServeStatic.parseRange("bytes=0-100,"));
        assertNull(ServeStatic.parseRange("bytes=,0-100"));
        assertNull(ServeStatic.parseRange("bytes=0-100,,200-300"));
    }

    @Test
    public void testParseRange_signedNumbersRejected() {
        // Explicit '+' / '-' signs must be rejected because RFC requires 1*DIGIT.
        assertNull(ServeStatic.parseRange("bytes=+0-100"));
        assertNull(ServeStatic.parseRange("bytes=0-+100"));
    }

    @Test
    public void testParseRange_overflow() {
        // 1e20 overflows long, must be rejected as malformed.
        assertNull(ServeStatic.parseRange("bytes=0-100000000000000000000"));
        assertNull(ServeStatic.parseRange("bytes=100000000000000000000-"));
        assertNull(ServeStatic.parseRange("bytes=-100000000000000000000"));
    }

    @Test
    public void testParseRange_largeButValidNumbers() {
        // Long.MAX_VALUE = 9223372036854775807
        List<?> ranges = ServeStatic.parseRange("bytes=0-9223372036854775807");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(0L, start(r));
        assertEquals(Long.MAX_VALUE, end(r));
        assertTrue(valid(r));
    }

    // ---------- 3. Whitespace and case-insensitive unit ----------

    @Test
    public void testParseRange_whitespaceTolerated() {
        // Leading/trailing/internal whitespace around each spec should be trimmed.
        List<?> ranges = ServeStatic.parseRange("bytes= 0 - 100 ");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(0L, start(r));
        assertEquals(100L, end(r));

        ranges = ServeStatic.parseRange("bytes= 0-100 , 200-300 ");
        assertNotNull(ranges);
        assertEquals(2, ranges.size());
        assertEquals(0L, start(ranges.getFirst()));
        assertEquals(100L, end(ranges.getFirst()));
        assertEquals(200L, start(ranges.get(1)));
        assertEquals(300L, end(ranges.get(1)));
    }

    @Test
    public void testParseRange_unitCaseInsensitive() {
        List<?> ranges = ServeStatic.parseRange("BYTES=0-100");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        assertEquals(0L, start(ranges.getFirst()));
        assertEquals(100L, end(ranges.getFirst()));

        ranges = ServeStatic.parseRange("Bytes=0-100");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
    }

    // ---------- 4. Validity flag semantics ----------

    @Test
    public void testParseRange_invalidRangeStillParsedButFlagged() {
        // start > end is syntactically valid but semantically invalid; parseRange
        // should still return the range with valid=false (validation is delegated
        // to the caller via Range#valid()).
        List<?> ranges = ServeStatic.parseRange("bytes=100-50");
        assertNotNull(ranges);
        assertEquals(1, ranges.size());
        Object r = ranges.getFirst();
        assertEquals(100L, start(r));
        assertEquals(50L, end(r));
        assertFalse(valid(r));
    }

    @Test
    public void testParseRange_reflectiveAccessSanity() {
        // Sanity check that the reflection helpers themselves work, so that a
        // failure in the other tests can be attributed to parseRange logic.
        List<?> ranges = ServeStatic.parseRange("bytes=0-0");
        try {
            assertNotNull(ranges);
            Object r = ranges.getFirst();
            assertEquals(0L, start(r));
            assertEquals(0L, end(r));
            assertTrue(valid(r));
        } catch (Exception e) {
            fail(e);
        }
    }

}
