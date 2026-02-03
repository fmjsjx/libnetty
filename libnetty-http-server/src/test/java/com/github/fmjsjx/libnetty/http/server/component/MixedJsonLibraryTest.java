package com.github.fmjsjx.libnetty.http.server.component;

import com.github.fmjsjx.libcommon.json.Fastjson2Library;
import com.github.fmjsjx.libcommon.json.Jackson3Library;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.component.JsonLibrary.EmptyWay;
import com.github.fmjsjx.libnetty.http.server.component.JsonLibrary.JsonReadException;
import com.github.fmjsjx.libnetty.http.server.component.JsonLibrary.JsonWriteException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MixedJsonLibrary}.
 */
public class MixedJsonLibraryTest {

    @Mock
    private HttpRequestContext mockContext;

    @Mock
    private FullHttpRequest mockRequest;

    private AutoCloseable mocks;

    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testBuilder() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .encoder(Jackson3Library.getInstance())
                .decoder(Jackson3Library.getInstance())
                .emptyWay(EmptyWay.NULL)
                .build();

        assertNotNull(library);
        assertEquals(EmptyWay.NULL, library.emptyWay());
    }

    @Test
    public void testRecommended() {
        MixedJsonLibrary library = MixedJsonLibrary.recommended();

        assertNotNull(library);
        assertEquals(EmptyWay.NULL, library.emptyWay());
    }

    @Test
    public void testRecommendedWithEmptyWay() {
        MixedJsonLibrary library = MixedJsonLibrary.recommended(EmptyWay.EMPTY);

        assertNotNull(library);
        assertEquals(EmptyWay.EMPTY, library.emptyWay());
    }

    @Test
    public void testBuilderWithCodec() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Fastjson2Library.getInstance())
                .build();

        assertNotNull(library);
    }

    @Test
    public void testBeforeRead() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeRead((ctx, content) -> content.replace("\"name\"", "\"modified\""))
                .build();

        String json = "{\"name\":\"test\",\"value\":100}";
        ByteBuf content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);

        when(mockContext.request()).thenReturn(mockRequest);
        when(mockRequest.content()).thenReturn(content);

        try {
            Map<?, ?> result = library.read(mockContext, Map.class);

            assertNotNull(result);
            assertEquals("test", result.get("modified"));
            assertNull(result.get("name"));
        } finally {
            content.release();
        }
    }

    @Test
    public void testBeforeEncode() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeEncode((ctx, value) -> {
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) value;
                        Map<String, Object> newMap = new HashMap<>(map);
                        newMap.put("encoded", true);
                        return newMap;
                    }
                    return value;
                })
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");
        ByteBuf result = library.write(mockContext, data);

        assertNotNull(result);
        String json = result.toString(CharsetUtil.UTF_8);
        assertTrue(json.contains("encoded"));
        assertTrue(json.contains("true"));

        result.release();
    }

    @Test
    public void testBeforeWrite() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeWrite((ctx, json) -> json.replace("test", "modified"))
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");
        ByteBuf result = library.write(mockContext, data);

        assertNotNull(result);
        String json = result.toString(CharsetUtil.UTF_8);
        assertTrue(json.contains("modified"));
        assertFalse(json.contains("test"));

        result.release();
    }

    @Test
    public void testBeforeEncodeAndBeforeWrite() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeEncode((ctx, value) -> {
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) value;
                        Map<String, Object> newMap = new HashMap<>(map);
                        newMap.put("step", "encode");
                        return newMap;
                    }
                    return value;
                })
                .beforeWrite((ctx, json) -> json.replace("encode", "write"))
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");
        ByteBuf result = library.write(mockContext, data);

        assertNotNull(result);
        String json = result.toString(CharsetUtil.UTF_8);
        assertTrue(json.contains("write"));
        assertFalse(json.contains("encode"));

        result.release();
    }

    @Test
    public void testReadInvalidJson() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .build();

        String invalidJson = "{invalid json}";
        ByteBuf content = Unpooled.copiedBuffer(invalidJson, CharsetUtil.UTF_8);

        try {
            assertThrows(JsonReadException.class, () -> library.read(content, Map.class));
        } finally {
            content.release();
        }
    }

    @Test
    public void testBeforeReadWithException() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeRead((ctx, content) -> {
                    throw new RuntimeException("beforeRead error");
                })
                .build();

        String json = "{\"name\":\"test\"}";
        ByteBuf content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);

        when(mockContext.request()).thenReturn(mockRequest);
        when(mockRequest.content()).thenReturn(content);

        try {
            assertThrows(JsonReadException.class, () -> library.read(mockContext, Map.class));
        } finally {
            content.release();
        }
    }

    @Test
    public void testBeforeEncodeWithException() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeEncode((ctx, value) -> {
                    throw new RuntimeException("beforeEncode error");
                })
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");

        assertThrows(JsonWriteException.class, () -> library.write(mockContext, data));
    }

    @Test
    public void testBeforeWriteWithException() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeWrite((ctx, json) -> {
                    throw new RuntimeException("beforeWrite error");
                })
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");

        assertThrows(JsonWriteException.class, () -> library.write(mockContext, data));
    }

    @Test
    public void testWriteNullValueWithBeforeEncode() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeEncode((ctx, value) -> {
                    // This should not be called when value is null
                    fail("beforeEncode should not be called for null value");
                    return value;
                })
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        ByteBuf result = library.write(mockContext, null);

        assertNotNull(result);
        String json = result.toString(CharsetUtil.UTF_8);
        assertEquals("null", json);

        result.release();
    }

    @Test
    public void testBeforeReadReturnNull() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeRead((ctx, content) -> null) // Return null to use original content
                .build();

        String json = "{\"name\":\"test\",\"value\":200}";
        ByteBuf content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);

        when(mockContext.request()).thenReturn(mockRequest);
        when(mockRequest.content()).thenReturn(content);

        try {
            Map<?, ?> result = library.read(mockContext, Map.class);

            assertNotNull(result);
            assertEquals("test", result.get("name"));
            assertEquals(200, result.get("value"));
        } finally {
            content.release();
        }
    }

    @Test
    public void testBeforeEncodeReturnNull() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeEncode((ctx, value) -> null) // Return null to use original value
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");
        ByteBuf result = library.write(mockContext, data);

        assertNotNull(result);
        String json = result.toString(CharsetUtil.UTF_8);
        assertTrue(json.contains("test"));

        result.release();
    }

    @Test
    public void testBeforeWriteReturnNull() {
        MixedJsonLibrary library = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .beforeWrite((ctx, json) -> null) // Return null to use original JSON
                .build();

        when(mockContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);

        Map<String, Object> data = Map.of("name", "test");
        ByteBuf result = library.write(mockContext, data);

        assertNotNull(result);
        String json = result.toString(CharsetUtil.UTF_8);
        assertTrue(json.contains("test"));

        result.release();
    }

    @Test
    public void testEmptyWayConfiguration() {
        MixedJsonLibrary nullWayLibrary = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .emptyWay(EmptyWay.NULL)
                .build();
        assertEquals(EmptyWay.NULL, nullWayLibrary.emptyWay());

        MixedJsonLibrary emptyWayLibrary = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .emptyWay(EmptyWay.EMPTY)
                .build();
        assertEquals(EmptyWay.EMPTY, emptyWayLibrary.emptyWay());

        MixedJsonLibrary errorWayLibrary = MixedJsonLibrary.builder()
                .codec(Jackson3Library.getInstance())
                .emptyWay(EmptyWay.ERROR)
                .build();
        assertEquals(EmptyWay.ERROR, errorWayLibrary.emptyWay());
    }

    @Test
    public void testComponentType() {
        MixedJsonLibrary library = MixedJsonLibrary.recommended();

        assertEquals(JsonLibrary.class, library.componentType());
    }


}
