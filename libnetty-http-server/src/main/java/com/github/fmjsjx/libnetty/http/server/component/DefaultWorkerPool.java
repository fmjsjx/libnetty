package com.github.fmjsjx.libnetty.http.server.component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * The default implementation of {@link WorkerPool}.
 * 
 * @since 1.3
 *
 * @author MJ Fang
 * 
 * @see WorkerPool
 */
public class DefaultWorkerPool implements WorkerPool {

    private static final int defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 8;

    private final ThreadPoolExecutor pool;

    /**
     * Constructs a new {@link DefaultWorkerPool} instance with the {@code defaultCorePoolSize}.
     */
    public DefaultWorkerPool() {
        this(defaultCorePoolSize);
    }

    /**
     * Constructs a new {@link DefaultWorkerPool} instance with the specified {@code corePoolSize} given.
     *
     * @param corePoolSize the number of threads to keep in the pool
     */
    public DefaultWorkerPool(int corePoolSize) {
        this(Math.min(defaultCorePoolSize, corePoolSize), new LinkedBlockingQueue<>());
    }

    /**
     * Constructs a new {@link DefaultWorkerPool} instance with the specified {@code corePoolSize} and the specified
     * {@code workQueue} given.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * @param workQueue    the queue to use for holding tasks before they are executed
     */
    public DefaultWorkerPool(int corePoolSize, BlockingQueue<Runnable> workQueue) {
        this(new ThreadPoolExecutor(corePoolSize, corePoolSize, 60, TimeUnit.SECONDS, workQueue,
                new DefaultThreadFactory("default-worker")));
    }

    DefaultWorkerPool(ThreadPoolExecutor pool) {
        this.pool = pool;
        pool.allowCoreThreadTimeOut(true);
    }

    @Override
    public Executor executor() {
        return pool;
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

}
