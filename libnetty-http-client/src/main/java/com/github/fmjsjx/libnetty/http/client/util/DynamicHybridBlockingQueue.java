package com.github.fmjsjx.libnetty.http.client.util;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A dynamic hybrid blocking queue which uses a pre-allocated {@link ArrayBlockingQueue}
 * as the primary queue ({@code primaryQueue}) under normal circumstances for its lower
 * memory overhead, and automatically activates a {@link LinkedBlockingQueue} as the
 * fallback queue ({@code fallbackQueue}) to keep accepting elements once the primary queue
 * is full and an offer fails.
 *
 * <p>For memory saving, when the given {@code primaryCapacity} is greater than
 * {@code MEMORY_SAVING_THRESHOLD} (512), no array is pre-allocated and a single
 * {@link LinkedBlockingQueue} is used instead (the primary queue and the fallback queue
 * refer to the same instance).</p>
 *
 * <p>Elements taken by {@link #take()} are preferred from the primary queue; once the
 * primary queue is drained empty, the internal state flag switches this queue into the
 * fallback-only mode so that only the fallback queue is used afterward (with a
 * double-check logic to keep the consuming order).</p>
 *
 * <p>Although this class is thread-safe (the {@code fallbackState} field is declared
 * {@code volatile} for visibility across threads), it is still recommended that only one
 * producer thread and one consumer thread be used with this queue.</p>
 *
 * @param <E> type of the elements
 * @author MJ Fang
 * @since 4.3
 */
public final class DynamicHybridBlockingQueue<E> {

    /**
     * The maximum primary queue capacity for which a pre-allocated {@link ArrayBlockingQueue}
     * is used; when the given capacity exceeds this threshold, the memory-saving mode is
     * enabled and a single {@link LinkedBlockingQueue} is used instead.
     */
    private static final int MEMORY_SAVING_THRESHOLD = 512;

    /**
     * State flag bit indicating that the fallback queue has been activated, i.e. the primary
     * queue was once full and new elements should be offered to the fallback queue.
     */
    private static final int FALLBACK_ACTIVATED = 1;

    /**
     * State flag bit indicating that only the fallback queue should be used, i.e. the primary
     * queue has been drained empty by {@link #take()}.
     */
    private static final int FALLBACK_ONLY = 2;

    /**
     * Combined state flag of {@code FALLBACK_ACTIVATED} and {@code FALLBACK_ONLY}, meaning
     * the fallback queue is activated, and only it should be used.
     */
    private static final int FALLBACK_ACTIVATED_ONLY = FALLBACK_ACTIVATED | FALLBACK_ONLY;

    /**
     * The primary queue, which is a pre-allocated {@link ArrayBlockingQueue} when the primary
     * capacity is small enough, or the same instance as {@code fallbackQueue} under the
     * memory-saving mode.
     */
    private final BlockingQueue<E> primaryQueue;

    /**
     * The fallback queue activated when the primary queue is full. Under the memory-saving
     * mode this is the only queue used (the same instance as {@code primaryQueue}).
     */
    private final LinkedBlockingQueue<E> fallbackQueue;

    /**
     * The state flag holding the {@code FALLBACK_ACTIVATED} and {@code FALLBACK_ONLY} bits.
     * Declared {@code volatile} to ensure the visibility of state transitions across threads.
     */
    private volatile int fallbackState;

    /**
     * Constructs a new {@code DynamicHybridBlockingQueue} with the primary queue capacity
     * given.
     *
     * <p>If {@code primaryCapacity} is less than or equal to {@code MEMORY_SAVING_THRESHOLD}
     * (512), a pre-allocated {@link ArrayBlockingQueue} with the given capacity is created as
     * the primary queue together with an initially empty {@link LinkedBlockingQueue} as the
     * fallback queue. Otherwise, for memory saving, no array is pre-allocated and a single
     * {@link LinkedBlockingQueue} with the given capacity is used as both the primary queue
     * and the fallback queue.</p>
     *
     * @param primaryCapacity the capacity of the primary queue
     */
    public DynamicHybridBlockingQueue(int primaryCapacity) {
        if (primaryCapacity <= MEMORY_SAVING_THRESHOLD) {
            primaryQueue = new ArrayBlockingQueue<>(primaryCapacity);
            fallbackQueue = new LinkedBlockingQueue<>();
        } else {
            fallbackQueue = new LinkedBlockingQueue<>(primaryCapacity);
            primaryQueue = fallbackQueue;
            fallbackState = FALLBACK_ACTIVATED_ONLY;
        }
    }

    /**
     * Returns {@code true} if the fallback queue has been activated, i.e. the
     * {@code FALLBACK_ACTIVATED} bit is set in the state flag.
     *
     * @return {@code true} if the fallback queue has been activated
     */
    private boolean isFallbackActivated() {
        return (fallbackState & FALLBACK_ACTIVATED) != 0;
    }

    /**
     * Returns {@code true} if only the fallback queue should be used, i.e. the
     * {@code FALLBACK_ONLY} bit is set in the state flag.
     *
     * @return {@code true} if only the fallback queue should be used
     */
    private boolean isFallbackOnly() {
        return (fallbackState & FALLBACK_ONLY) != 0;
    }

    /**
     * Inserts the specified element into this queue without blocking.
     *
     * <p>If the fallback queue has already been activated, the element is offered to the
     * fallback queue directly. Otherwise, the element is offered to the primary queue first;
     * only when the primary queue is full (i.e. the offer fails) is the fallback queue
     * activated by setting the state flag to {@code FALLBACK_ACTIVATED} and the element then
     * offered to the fallback queue instead.</p>
     *
     * @param e the element to insert
     */
    public void offer(E e) {
        if (isFallbackActivated()) {
            fallbackQueue.offer(e);
            return;
        }
        if (!primaryQueue.offer(e)) {
            fallbackState = FALLBACK_ACTIVATED;
            fallbackQueue.offer(e);
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary until an element
     * becomes available.
     *
     * <p>Elements are taken from the primary queue by default. If the fallback queue has been
     * activated but this queue has not yet switched into the fallback-only mode, the primary
     * queue is polled first (double-check) so that any elements remaining in it are consumed
     * before the fallback queue; once the primary queue is drained empty, the state flag is
     * switched to {@code FALLBACK_ACTIVATED_ONLY} and all subsequent takings go to the
     * fallback queue only.</p>
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    public E take() throws InterruptedException {
        // fallbackQueue is used
        if (isFallbackActivated()) {
            // check if only fallbackQueue is used
            if (isFallbackOnly()) {
                return fallbackQueue.take();
            }
            // double-check
            E e = primaryQueue.poll();
            if (e != null) {
                if (primaryQueue.isEmpty()) {
                    fallbackState = FALLBACK_ACTIVATED_ONLY;
                }
                return e;
            }
            fallbackState = FALLBACK_ACTIVATED_ONLY;
            return fallbackQueue.take();
        }
        // fallbackQueue is not used, so just take from primaryQueue
        return primaryQueue.take();
    }

}
