package com.github.fmjsjx.libnetty.fastcgi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBufAllocator;

/**
 * unit tests for util functions
 *
 * @author eharman
 * @since 2021-10-05
 */
public class FcgiCodecUtilTest {

    @Test
    public void variableLengthIO() {
        for (int i = 0; i < 2100; i++) {
            var buf = ByteBufAllocator.DEFAULT.buffer();
            FcgiCodecUtil.encodeVariableLength(i, buf);
            var out = FcgiCodecUtil.decodeVariableLength(buf);
            assertEquals(i, out);
        }
    }

}
