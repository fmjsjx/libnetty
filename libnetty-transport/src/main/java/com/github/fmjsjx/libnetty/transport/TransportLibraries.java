package com.github.fmjsjx.libnetty.transport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static factory class for {@link TransportLibrary}.
 * 
 * @since 1.0
 * 
 * @author MJ Fang
 */

final class TransportLibraries {
    
    private static final Logger log = LoggerFactory.getLogger(TransportLibraries.class);

    private static final class DefaultLibraryHolder {
        private static final TransportLibrary defaultLibrary;

        static {
            boolean ioUringAvailable = false;
            try {
                Class<?> ioUring = Class.forName("io.netty.channel.uring.IoUring");
                ioUringAvailable = isAvailable(ioUring);
            } catch (ClassNotFoundException e) {
                log.info("IoUring not found, start without optional native library IoUringLibrary");
            }
            if (ioUringAvailable) {
                defaultLibrary = IoUringTransportLibrary.getInstance();
            } else {
                boolean epollAvailable = false;
                try {
                    Class<?> epoll = Class.forName("io.netty.channel.epoll.Epoll");
                    epollAvailable = isAvailable(epoll);
                } catch (ClassNotFoundException e) {
                    log.info("Epoll not found, start without optional native library EpollLibrary");
                }
                if (epollAvailable) {
                    defaultLibrary = EpollTransportLibrary.getInstance();
                } else {
                    boolean kqueueAvailable = false;
                    try {
                        Class<?> kqueue = Class.forName("io.netty.channel.kqueue.KQueue");
                        kqueueAvailable = isAvailable(kqueue);
                    } catch (ClassNotFoundException e) {
                        log.info("KQueue not found, start without optional native library KQueueLibrary");
                    }
                    if (kqueueAvailable) {
                        defaultLibrary = KQueueTransportLibrary.getInstance();
                    } else {
                        defaultLibrary = NioTransportLibrary.getInstance();
                    }
                }
            }
        }

    }

    private static final boolean isAvailable(Class<?> library) {
        try {
            Method method = library.getMethod("isAvailable");
            return (Boolean) method.invoke(library);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            return false;
        }
    }

    /**
     * Returns default {@link TransportLibrary} instance, native library is
     * preferred.
     * 
     * @return the default {@link TransportLibrary}
     */
    static final TransportLibrary getDefault() {
        return DefaultLibraryHolder.defaultLibrary;
    }

    private TransportLibraries() {}
    
}
