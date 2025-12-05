package com.github.fmjsjx.libnetty.http.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AbstractHttpClientTests {

    @Test
    public void testDefaultUserAgentValue() {
        var value = AbstractHttpClient.DEFAULT_USER_AGENT_VALUE;
        assertNotNull(value);
        try (var in = getClass().getResourceAsStream("/default-user-agent")) {
            assertNotNull(in);
            var expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            assertEquals(expected, value.toString());
        } catch (IOException e) {
            fail(e);
        }
    }

}
