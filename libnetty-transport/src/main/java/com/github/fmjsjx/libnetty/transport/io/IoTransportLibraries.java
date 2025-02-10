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
                    } else {
                        defaultLibrary = NioIoTransportLibrary.getInstance();
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
     * Returns default {@link IoTransportLibrary} instance, native library is
     * preferred.
     *
     * @return the default {@link IoTransportLibrary}
     */
    static final IoTransportLibrary getDefault() {
        return DefaultLibraryHolder.defaultLibrary;
    }

    private IoTransportLibraries() {
    }

}
