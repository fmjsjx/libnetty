package com.github.fmjsjx.libnetty.handler.ssl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.handler.ssl.SslContext;
import io.netty.util.DomainWildcardMappingBuilder;
import io.netty.util.Mapping;

/**
 * Implementations of {@link SniHandlerProvider}.
 * 
 * @author MJ Fang
 * 
 * @since 2.3
 */
public class SniHandlerProviders {

    /**
     * Creates a new {@link SniHandlerProvider} instance with the specified
     * {@code mapping} given.
     * 
     * @param mapping the mapping of domain name to SslContext
     * @return a {@code SniHandlerProvider}
     */
    public static final SniHandlerProvider create(Mapping<? super String, ? extends SslContext> mapping) {
        return new DefaultSniHandlerProvider(mapping);
    }

    /**
     * Creates a new {@link SniHandlerProvider} instance with the specified
     * {@code defaultSslContext} and the specified {@code mapping} given.
     * 
     * @param defaultSslContext the default value for {@link Mapping#map(Object)} to
     *                          return when nothing matches the input
     * @param mapping           the map holding the mapping of domain name to
     *                          SslContext
     * @return a {@code SniHandlerProvider}
     */
    public static final SniHandlerProvider create(SslContext defaultSslContext, Map<String, SslContext> mapping) {
        var builder = new DomainWildcardMappingBuilder<>(defaultSslContext);
        mapping.forEach(builder::add);
        return create(builder.build());
    }

    /**
     * Creates a new {@link PermutableSniHandlerProvider} instance with the
     * specified {@code mapping} given.
     * 
     * @param mapping the initial mapping of domain name to SslContext
     * @return a {@code PermutableSniHandlerProvider}
     */
    public static final PermutableSniHandlerProvider permutable(Mapping<? super String, ? extends SslContext> mapping) {
        return new PermutableSniHandlerProviderImpl(mapping);
    }

    private static final class PermutableSniHandlerProviderImpl extends AbstractSniHandlerProvider
            implements PermutableSniHandlerProvider {

        private final AtomicReference<Mapping<? super String, ? extends SslContext>> mapping;

        private PermutableSniHandlerProviderImpl(Mapping<? super String, ? extends SslContext> mapping) {
            Objects.requireNonNull(mapping, "mapping must not be null");
            this.mapping = new AtomicReference<>(mapping);
        }

        @Override
        public Mapping<? super String, ? extends SslContext> mapping() {
            return mapping.get();
        }

        @Override
        public Mapping<? super String, ? extends SslContext> setMapping(
                Mapping<? super String, ? extends SslContext> mapping) {
            Objects.requireNonNull(mapping, "mapping must not be null");
            return this.mapping.getAndSet(mapping);
        }

    }

}
