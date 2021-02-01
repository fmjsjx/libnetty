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
 */
public interface WorkerPool extends HttpServerComponent {

    @Override
    default Class<WorkerPool> componentType() {
        return WorkerPool.class;
    }

    Executor executor();

    @Override
    default void close() throws Exception {
        shutdown();
    }

    void shutdown();

}
