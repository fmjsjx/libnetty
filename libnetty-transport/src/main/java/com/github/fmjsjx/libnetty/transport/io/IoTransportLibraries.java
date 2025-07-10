package com.github.fmjsjx.libnetty.transport.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Static factory class for {@link IoTransportLibrary}.
 *
 * @author MJ Fang
 * @since 3.8
 */

final class IoTransportLibraries {

    private static final Logger log = LoggerFactory.getLogger(IoTransportLibraries.class);

    private static final class DefaultLibraryHolder {
        private static final IoTransportLibrary defaultLibrary;

        static {
            boolean ioUringAvailable = false;
            try {
                Class<?> ioUring = Class.forName("io.netty.channel.uring.IoUring");
                ioUringAvailable = isAvailable(ioUring);
            } catch (ClassNotFoundException e) {
                log.info("IoUring not found, start without optional native library IoUringLibrary");
            }
            if (ioUringAvailable) {
                defaultLibrary = IoUringIoTransportLibrary.getInstance();
                log.info("io_uring is available, sets the native library IoUringLibrary as the default library");
            } else {
                boolean epollAvailable = false;
                try {
                    Class<?> epoll = Class.forName("io.netty.channel.epoll.Epoll");
                    epollAvailable = isAvailable(epoll);
                } catch (ClassNotFoundException e) {
                    log.info("Epoll not found, start without optional native library EpollLibrary");
                }
                if (epollAvailable) {
                    defaultLibrary = EpollIoTransportLibrary.getInstance();
                    log.info("epoll is available, sets the native library EpollLibrary as the default library");
                } else {
                    boolean kqueueAvailable = false;
                    try {
                        Class<?> kqueue = Class.forName("io.netty.channel.kqueue.KQueue");
                        kqueueAvailable = isAvailable(kqueue);
                    } catch (ClassNotFoundException e) {
                        log.info("KQueue not found, start without optional native library KQueueLibrary");
                    }
                    if (kqueueAvailable) {
                        defaultLibrary = KQueueIoTransportLibrary.getInstance();
                        log.info("kqueue is available, sets the native library KQueueLibrary as the default library");
                    } else {
                        defaultLibrary = NioIoTransportLibrary.getInstance();
                        log.info("All native libraries are unavailable, sets the NioIoTransportLibrary as the default library");
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
     * Returns the default {@link IoTransportLibrary} instance, a native
     * library is preferred.
     *
     * @return the default {@link IoTransportLibrary}
     */
    static final IoTransportLibrary getDefault() {
        return DefaultLibraryHolder.defaultLibrary;
    }

    private IoTransportLibraries() {
    }

}
