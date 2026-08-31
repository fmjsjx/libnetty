package com.github.fmjsjx.libnetty.http.client.util;

import java.lang.reflect.Field;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicHybridBlockingQueueTests {

    private static final int MEMORY_SAVING_THRESHOLD = 512;

    private static final int FALLBACK_ACTIVATED = 1;

    private static final int FALLBACK_ONLY = 2;

    private static final int FALLBACK_ACTIVATED_ONLY = FALLBACK_ACTIVATED | FALLBACK_ONLY;

    private static Object field(DynamicHybridBlockingQueue<?> queue, String name) {
        try {
            Field f = DynamicHybridBlockingQueue.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(queue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static BlockingQueue<?> primaryQueue(DynamicHybridBlockingQueue<?> queue) {
        return (BlockingQueue<?>) field(queue, "primaryQueue");
    }

    private static BlockingQueue<?> fallbackQueue(DynamicHybridBlockingQueue<?> queue) {
        return (BlockingQueue<?>) field(queue, "fallbackQueue");
    }

    private static int fallbackState(DynamicHybridBlockingQueue<?> queue) {
        return (Integer) field(queue, "fallbackState");
    }

    // ---------- 1. Normal mode (primaryCapacity <= 512) ----------

    @Test
    public void testNormalMode_initialState() {
        var queue = new DynamicHybridBlockingQueue<String>(4);
        assertInstanceOf(ArrayBlockingQueue.class, primaryQueue(queue));
        assertInstanceOf(LinkedBlockingQueue.class, fallbackQueue(queue));
        assertEquals(0, fallbackState(queue));
    }

    @Test
    public void testNormalMode_offerBeforePrimaryFull() {
        var queue = new DynamicHybridBlockingQueue<String>(4);
        queue.offer("a");
        queue.offer("b");
        // elements are stored in the primary queue only
        assertEquals(2, primaryQueue(queue).size());
        assertTrue(primaryQueue(queue).contains("a"));
        assertTrue(primaryQueue(queue).contains("b"));
        assertEquals(0, fallbackQueue(queue).size());
        // fallback queue is not activated yet
        assertEquals(0, fallbackState(queue));
    }

    @Test
    public void testNormalMode_offerActivatesFallback() {
        var queue = new DynamicHybridBlockingQueue<String>(2);
        queue.offer("a");
        queue.offer("b");
        assertEquals(0, fallbackState(queue));
        // the primary queue is full now, so the next offer activates the fallback queue
        queue.offer("c");
        assertEquals(FALLBACK_ACTIVATED, fallbackState(queue));
        // "c" goes into the fallback queue instead
        assertEquals(2, primaryQueue(queue).size());
        assertEquals(1, fallbackQueue(queue).size());
        assertTrue(fallbackQueue(queue).contains("c"));
        // subsequent offers go to the fallback queue directly
        queue.offer("d");
        assertEquals(2, primaryQueue(queue).size());
        assertEquals(2, fallbackQueue(queue).size());
        assertTrue(fallbackQueue(queue).contains("d"));
        assertEquals(FALLBACK_ACTIVATED, fallbackState(queue));
    }

    @Test
    public void testNormalMode_takeWithoutFallbackActivated() throws InterruptedException {
        var queue = new DynamicHybridBlockingQueue<String>(4);
        queue.offer("a");
        queue.offer("b");
        // takes come from the primary queue while the fallback is not activated
        assertEquals("a", queue.take());
        assertEquals("b", queue.take());
        assertEquals(0, primaryQueue(queue).size());
        assertEquals(0, fallbackQueue(queue).size());
        assertEquals(0, fallbackState(queue));
    }

    @Test
    public void testNormalMode_takePrefersPrimaryQueue() throws InterruptedException {
        var queue = new DynamicHybridBlockingQueue<String>(2);
        queue.offer("a");
        queue.offer("b");
        queue.offer("c"); // activates the fallback queue
        assertEquals(FALLBACK_ACTIVATED, fallbackState(queue));
        // take() prefers the primary queue while it is not empty
        assertEquals("a", queue.take());
        assertEquals(FALLBACK_ACTIVATED, fallbackState(queue));
        // taking the last element of the primary queue switches to fallback-only
        assertEquals("b", queue.take());
        assertEquals(FALLBACK_ACTIVATED_ONLY, fallbackState(queue));
        // subsequent takings go to the fallback queue only
        assertEquals("c", queue.take());
        assertEquals(FALLBACK_ACTIVATED_ONLY, fallbackState(queue));
        assertEquals(0, primaryQueue(queue).size());
        assertEquals(0, fallbackQueue(queue).size());
    }

    @Test
    public void testNormalMode_offerAfterFallbackOnly() throws InterruptedException {
        var queue = new DynamicHybridBlockingQueue<String>(1);
        queue.offer("a");
        queue.offer("b"); // activates the fallback queue
        queue.offer("c"); // goes to the fallback queue directly
        // draining the primary queue switches to fallback-only
        assertEquals("a", queue.take());
        assertEquals(FALLBACK_ACTIVATED_ONLY, fallbackState(queue));
        // new elements still go to the fallback queue after switched to fallback-only
        queue.offer("d");
        assertEquals(0, primaryQueue(queue).size());
        assertEquals(3, fallbackQueue(queue).size());
        // the FIFO order of the fallback queue is kept
        assertEquals("b", queue.take());
        assertEquals("c", queue.take());
        assertEquals("d", queue.take());
        assertEquals(FALLBACK_ACTIVATED_ONLY, fallbackState(queue));
        assertEquals(0, fallbackQueue(queue).size());
    }

    @Test
    public void testNormalMode_boundaryThreshold() {
        // capacity == MEMORY_SAVING_THRESHOLD stays in the normal mode
        var queue = new DynamicHybridBlockingQueue<String>(MEMORY_SAVING_THRESHOLD);
        assertInstanceOf(ArrayBlockingQueue.class, primaryQueue(queue));
        assertInstanceOf(LinkedBlockingQueue.class, fallbackQueue(queue));
        assertEquals(0, fallbackState(queue));
    }

    // ---------- 2. Memory-saving mode (primaryCapacity > 512) ----------

    @Test
    public void testMemorySavingMode_initialState() {
        var queue = new DynamicHybridBlockingQueue<String>(MEMORY_SAVING_THRESHOLD + 1);
        // the primary queue and the fallback queue refer to the same instance
        assertSame(primaryQueue(queue), fallbackQueue(queue));
        assertInstanceOf(LinkedBlockingQueue.class, primaryQueue(queue));
        assertInstanceOf(LinkedBlockingQueue.class, fallbackQueue(queue));
        // the fallback-only state is set from the beginning
        assertEquals(FALLBACK_ACTIVATED_ONLY, fallbackState(queue));
    }

    @Test
    public void testMemorySavingMode_offerAndTake() throws InterruptedException {
        var queue = new DynamicHybridBlockingQueue<String>(MEMORY_SAVING_THRESHOLD + 1);
        queue.offer("a");
        queue.offer("b");
        // all elements are stored in the single linked queue
        assertEquals(2, primaryQueue(queue).size());
        assertEquals(2, fallbackQueue(queue).size());
        // takes come from the single queue in FIFO order
        assertEquals("a", queue.take());
        assertEquals("b", queue.take());
        assertEquals(0, primaryQueue(queue).size());
        assertEquals(0, fallbackQueue(queue).size());
        assertEquals(FALLBACK_ACTIVATED_ONLY, fallbackState(queue));
    }

}
