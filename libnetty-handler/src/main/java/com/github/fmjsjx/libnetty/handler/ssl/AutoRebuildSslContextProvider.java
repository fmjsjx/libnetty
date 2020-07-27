package com.github.fmjsjx.libnetty.handler.ssl;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * The abstract implementation for {@link SslContextProvider} with a
 * {@link WatchService} to auto-rebuild {@link SSLContext} when certificates
 * modified.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public abstract class AutoRebuildSslContextProvider implements SslContextProvider {

    private static final Logger log = LoggerFactory.getLogger(AutoRebuildSslContextProvider.class);

    private WatchService watchService;
    private ExecutorService executor;

    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<SslContext> sslContextRef = new AtomicReference<>();

    private final Path dir;
    private final Set<String> watchingFiles;

    /**
     * Constructs a new {@link AutoRebuildSslContextProvider} with the specified
     * directory {@link Path} and watching files given.
     * 
     * @param factory       a factory to create {@link SslContext}s
     * @param dir           the path of the parent directory
     * @param watchingFiles the collection contains the name of each watching files
     * @throws SSLRuntimeException if any SSL error occurs
     * @throws IOException         if an I/O error occurs
     */
    protected AutoRebuildSslContextProvider(SslContextProvider factory, Path dir, Collection<String> watchingFiles)
            throws SSLRuntimeException, IOException {
        this(factory, dir, Collections.unmodifiableSet(new LinkedHashSet<>(watchingFiles)));
    }

    /**
     * Constructs a new {@link AutoRebuildSslContextProvider} with the specified
     * directory {@link Path} and watching files given.
     * 
     * @param factory       a factory to create {@link SslContext}s
     * @param dir           the path of the parent directory
     * @param watchingFiles the array contains the name of each watching files
     * @throws SSLRuntimeException if any SSL error occurs
     * @throws IOException         if an I/O error occurs
     */
    protected AutoRebuildSslContextProvider(SslContextProvider factory, Path dir, String... watchingFiles)
            throws SSLRuntimeException, IOException {
        this(factory, dir, Arrays.asList(watchingFiles));
    }

    private AutoRebuildSslContextProvider(SslContextProvider factory, Path dir, Set<String> watchingFiles)
            throws SSLRuntimeException, IOException {
        this.dir = dir;
        this.watchingFiles = watchingFiles;
        SslContext first = factory.get();
        sslContextRef.set(first);
        watchService = dir.getFileSystem().newWatchService();
        // register ENTRY_CREATE to support the case when user removed old files before
        // set the new file
        dir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE);
        executor = Executors.newSingleThreadExecutor(new DefaultThreadFactory("watch-files", true));
        executor.execute(new WatchingTask(factory));
    }

    /**
     * Returns the {@link Path} of the watching directory.
     * 
     * @return the path of the watching directory
     */
    public Path getDir() {
        return dir;
    }

    /**
     * Returns the set contains the name of each watching files.
     * 
     * @return the set contains the name of each watching file
     */
    public Set<String> watchingFiles() {
        return watchingFiles;
    }

    /**
     * Returns a {@link Stream} contains the resolved path of each watching files.
     * 
     * @return a {@code Stream<Path>}
     */
    public Stream<Path> resolvedWatchingFiles() {
        return watchingFiles.stream().map(dir::resolve);
    }

    @Override
    public SslContext get() {
        return sslContextRef.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            // can only close once
            if (executor != null) {
                executor.shutdown();
            }
            if (watchService != null) {
                watchService.close();
            }
        }
    }

    private class WatchingTask implements Runnable {

        private final SslContextProvider factory;

        public WatchingTask(SslContextProvider factory) {
            this.factory = factory;
        }

        @Override
        public void run() {
            for (; !isClosed();) {
                try {
                    WatchKey watchKey = watchService.poll(30, TimeUnit.SECONDS);
                    if (watchKey != null) {
                        boolean needRebuild = false;
                        try {
                            List<WatchEvent<?>> events = watchKey.pollEvents();
                            for (WatchEvent<?> watchEvent : events) {
                                String fileName = watchEvent.context().toString();
                                log.debug("Polled event {} ==> {}", watchEvent.kind(), fileName);
                                if (watchingFiles.contains(fileName)) {
                                    needRebuild = true;
                                }
                            }
                        } finally {
                            watchKey.reset();
                        }
                        if (needRebuild) {
                            Thread.sleep(100L); // sleep 100 milliseconds to wait all IO operations finished
                            SslContext sslContext = factory.get();
                            SslContext old = sslContextRef.getAndSet(sslContext);
                            log.info("Auto rebuild SslContext: {} => {}", old, sslContext);
                        }
                    }
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    // ignore
                } catch (SSLRuntimeException e) {
                    log.error("Unexpected error occurs when build SslContext", e);
                }
            }
        }
    }

}
