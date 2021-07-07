package com.github.fmjsjx.libnetty.http.server.component;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * The wrapped implementation of {@link WorkerPool}.
 * 
 * @since 2.2
 * @see WorkerPool
 */
public class WrappedWorkerPool implements WorkerPool {

    private final Executor wrapped;
    private final boolean cascade;

    /**
     * Constructs a new {@link WrappedWorkerPool} with the specified {@code wrapped}
     * executor.
     * 
     * @param wrapped the wrapped executor
     */
    public WrappedWorkerPool(Executor wrapped) {
        this(wrapped, true);
    }

    /**
     * Constructs a new {@link WrappedWorkerPool} with the specified {@code wrapped}
     * executor.
     * 
     * @param wrapped the wrapped executor
     * @param cascade if the wrapped executor will be cascaded shutdown or not
     */
    public WrappedWorkerPool(Executor wrapped, boolean cascade) {
        this.wrapped = wrapped;
        this.cascade = cascade && wrapped instanceof ExecutorService;
    }

    @Override
    public Executor executor() {
        return wrapped;
    }

    @Override
    public void shutdown() {
        if (cascade) {
            ((ExecutorService) wrapped).shutdown();
        }
    }

}
