package com.github.fmjsjx.libnetty.http.server.component;

import java.util.concurrent.Executor;

/**
 * Provides a worker thread pool.
 * 
 * @since 1.3
 *
 * @author MJ Fang
 * 
 * @see DefaultWorkerPool
 * @see WrappedWorkerPool
 */
public interface WorkerPool extends HttpServerComponent {

    @Override
    default Class<WorkerPool> componentType() {
        return WorkerPool.class;
    }

    /**
     * Returns the executor.
     * 
     * @return the executor
     */
    Executor executor();

    @Override
    default void close() throws Exception {
        shutdown();
    }

    /**
     * Shutdown this worker pool.
     */
    void shutdown();

}
