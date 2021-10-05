package com.github.fmjsjx.libnetty.fastcgi;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * unit tests for util functions
 *
 * @author eharman
 * @since 2021-10-05
 */
public class FcgiCodecUtilTest
{
    @Test
    void variableLengthIO() {
        for (int i = 0; i < 2100; i++)
        {
            var buf = Unpooled.buffer();
            FcgiCodecUtil.encodeVariableLength(i, buf);
            var out = FcgiCodecUtil.decodeVariableLength(buf);
            assertEquals(i, out);
        }
    }
}
