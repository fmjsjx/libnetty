package com.github.fmjsjx.libnetty.http.server.component;

import java.util.Objects;

/**
 * The abstract implementation of {@link JsonLibrary}.
 *
 * @author MJ Fang
 * @since 3.6
 */
public abstract class AbstractJsonLibrary implements JsonLibrary {

    protected final EmptyWay emptyWay;

    /**
     * Construct with the specified {@link EmptyWay} given.
     *
     * @param emptyWay the {@code EmptyWay}
     */
    protected AbstractJsonLibrary(final EmptyWay emptyWay) {
        this.emptyWay = Objects.requireNonNull(emptyWay, "emptyWay must not be null");
    }

    @Override
    public final Class<JsonLibrary> componentType() {
        return JsonLibrary.class;
    }

    @Override
    public final EmptyWay emptyWay() {
        return emptyWay;
    }

}
