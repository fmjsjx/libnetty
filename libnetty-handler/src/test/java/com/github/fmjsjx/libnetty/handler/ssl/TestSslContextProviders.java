package com.github.fmjsjx.libnetty.handler.ssl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.netty.handler.ssl.SslContext;

public class TestSslContextProviders {

    @Test
    public void testSelfSignedForServer() {
        try {
            SslContextProvider provider = SslContextProviders.selfSignedForServer();
            assertNotNull(provider);
            SslContext sslContext = provider.get();
            assertNotNull(sslContext);
            assertTrue(sslContext.isServer());
        } catch (Exception e) {
            fail(e);
        }
    }

}
