package com.github.fmjsjx.libnetty.transport.io;

import io.netty.channel.IoHandlerFactory;

import java.util.function.Supplier;

/**
 * The abstract implementation of {@link IoTransportLibrary}.
 *
 * @author MJ Fang
 * @since 3.8
 */
abstract class AbstractIoTransportLibrary implements IoTransportLibrary {

    protected final Supplier<IoHandlerFactory> factoryCreator;

    /**
     * Constructs a new {@link AbstractIoTransportLibrary} with the
     * specified {@code factoryCreator} that creates
     * {@link IoHandlerFactory}s
     *
     * @param factoryCreator the creator creates {@link IoHandlerFactory}s
     */
    protected AbstractIoTransportLibrary(Supplier<IoHandlerFactory> factoryCreator) {
        this.factoryCreator = factoryCreator;
    }

    @Override
    public IoHandlerFactory createIoHandlerFactory() {
        return factoryCreator.get();
    }
}
