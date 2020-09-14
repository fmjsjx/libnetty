package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.fmjsjx.libnetty.http.server.HttpResult;

public class AccessLoggerTest {

    @Test
    public void testLogMapperPerf() {
        String p = "Hello :datetime :method :path :version :remote-address - :status :response-time ms :result-length World!";
        System.out.println("test pattern string: " + p);
        Function<HttpResult, String> m1 = AccessLogger.generateMapperFromPattern(p);
        Function<HttpResult, String> m2 = AccessLogger.generateMapperFromPattern2(p);
        Function<HttpResult, String> m3 = AccessLogger.generateMapperFromPattern3(p);
        System.out.println(m1.apply(null));
        System.out.println(m2.apply(null));
        System.out.println(m3.apply(null));
        System.out.println("-- warm --");
        int count = 10_000_000;
        for (int i = 0; i < count; i++) {
            m1.apply(null);
            m2.apply(null);
            m3.apply(null);
        }
        System.out.println("-- start --");
        long[] ns = new long[10];
        long[] ns2 = new long[ns.length];
        long[] ns3 = new long[ns.length];
        long n;
        for (int i = 0; i < ns.length; i++) {
            n = System.nanoTime();
            for (int j = 0; j < count; j++) {
                m1.apply(null);
            }
            ns[i] = System.nanoTime() - n;

            n = System.nanoTime();
            for (int j = 0; j < count; j++) {
                m2.apply(null);
            }
            ns2[i] = System.nanoTime() - n;

            n = System.nanoTime();
            for (int j = 0; j < count; j++) {
                m3.apply(null);
            }
            ns3[i] = System.nanoTime() - n;
        }
        System.out.println("-- result --");
        Arrays.sort(ns);
        Arrays.sort(ns2);
        Arrays.sort(ns3);
        for (long n1 : ns) {
            System.out.println(n1);
        }
        System.out.println("-- method 1 --");

        for (long n2 : ns2) {
            System.out.println(n2);
        }
        System.out.println("-- method 2 --");

        for (long n3 : ns3) {
            System.out.println(n3);
        }
        System.out.println("-- method 3 --");
    }
    
}
