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

    private static final int defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private static final int defaultMaximumPoolSize = Runtime.getRuntime().availableProcessors() * 8;

    private final ThreadPoolExecutor pool;

    public DefaultWorkerPool() {
        this(defaultMaximumPoolSize);
    }

    public DefaultWorkerPool(int maximumPoolSize) {
        this(Math.min(defaultCorePoolSize, maximumPoolSize), maximumPoolSize);
    }

    public DefaultWorkerPool(int corePoolSize, int maximumPoolSize) {
        this(corePoolSize, maximumPoolSize, new LinkedBlockingQueue<>());
    }

    public DefaultWorkerPool(int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
        this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60, TimeUnit.SECONDS, workQueue,
                new DefaultThreadFactory("default-worker")));
    }

    DefaultWorkerPool(ThreadPoolExecutor pool) {
        this.pool = pool;
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
