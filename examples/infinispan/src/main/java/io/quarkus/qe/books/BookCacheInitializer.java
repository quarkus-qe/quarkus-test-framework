package io.quarkus.qe.books;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.StringConfiguration;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class BookCacheInitializer {

    public static final String CACHE_NAME = "booksCache";

    private static final String CACHE_CONFIG = "<infinispan><cache-container>"
            + "<distributed-cache name=\"%s\"></distributed-cache>"
            + "</cache-container></infinispan>";

    @Inject
    RemoteCacheManager cacheManager;

    void onStart(@Observes StartupEvent ev) {
        cacheManager.administration().getOrCreateCache(CACHE_NAME,
                new StringConfiguration(String.format(CACHE_CONFIG, CACHE_NAME)));

    }

}
