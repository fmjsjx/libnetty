package com.github.fmjsjx.libnetty.http.server.component;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The implementation of {@link WorkerPool} using virtual threads.
 *
 * @author MJ Fang
 * @see WorkerPool
 * @since 4.0
 */
public class VirtualThreadWorkerPool implements WorkerPool {

    private final String poolName;
    private final ExecutorService executor;

    /**
     * Constructs a new {@link VirtualThreadWorkerPool} with the
     * default pool name.
     */
    public VirtualThreadWorkerPool() {
        this("v-worker");
    }

    /**
     * Constructs a new {@link VirtualThreadWorkerPool} with the
     * specified pool name given.
     *
     * @param poolName the pool name
     */
    public VirtualThreadWorkerPool(String poolName) {
        this.poolName = Objects.requireNonNull(poolName, "poolName must not be null");
        this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(poolName, 0).factory());
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public String toString() {
        return "VirtualThreadWorkerPool(poolName=" + poolName + ", executor=" + executor + ")";
    }
}
