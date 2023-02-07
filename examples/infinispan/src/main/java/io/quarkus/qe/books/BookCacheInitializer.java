package io.quarkus.qe.books;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

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
                new XMLStringConfiguration(String.format(CACHE_CONFIG, CACHE_NAME)));

    }

}
