package com.jasonbertolo.urlshortener.api.component;

import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import com.jasonbertolo.urlshortener.api.repository.ShortUrlRepository;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static org.owasp.esapi.Logger.*;

@Component
public class ScheduledMaintenance {

    private static final Logger LOGGER = ESAPI.getLogger(ScheduledMaintenance.class.getSimpleName());

    private final ShortUrlRepository shortUrlRepository;
    private final ReactiveRedisOperations<String, ShortUrl> reactiveRedisOperations;
    private final UrlShortenerSettings urlShortenerSettings;

    @Autowired
    public ScheduledMaintenance(ShortUrlRepository shortUrlRepository,
                                ReactiveRedisOperations<String, ShortUrl> reactiveRedisOperations,
                                UrlShortenerSettings urlShortenerSettings) {
        this.shortUrlRepository = shortUrlRepository;
        this.reactiveRedisOperations = reactiveRedisOperations;
        this.urlShortenerSettings = urlShortenerSettings;
    }

    @Scheduled(cron = "${url-shortener.scheduled-maintenance.cleanup-cron}",
            zone = "${url-shortener.scheduled-maintenance.cron-zone}")
    public void cleanup() {
        if (!urlShortenerSettings.getScheduledMaintenance().isCleanupEnabled()) {
            LOGGER.warning(EVENT_UNSPECIFIED, "Clean Up for expired ShortUrls is disabled");
            return;
        }

        LOGGER.debug(EVENT_UNSPECIFIED, "Checking for expired ShortUrls...");
        // Find expired ShortUrls
        shortUrlRepository.findByExpiresAtBefore(Instant.now()).flatMap(shortUrl -> {
            LOGGER.info(EVENT_UNSPECIFIED, String.format("ShortUrl id[%s] expired, removing it", shortUrl.getId()));
            // Delete from cache
            return reactiveRedisOperations.delete(shortUrl.getKey())
                    .doOnSuccess(s -> LOGGER.info(EVENT_SUCCESS, String.format(
                            "ShortUrl id[%s] was deleted from cache", shortUrl.getId())))
                    .doOnError(e -> LOGGER.warning(EVENT_FAILURE, String.format(
                            "Error deleting ShortUrl id[%s] from cache: [%s]", shortUrl.getId(), e.getMessage())))
                    .thenReturn(shortUrl)
                    .onErrorReturn(shortUrl)

                    // Delete from mongo
                    .then(shortUrlRepository.delete(shortUrl))
                    .doOnSuccess(s -> LOGGER.info(EVENT_SUCCESS, String.format(
                            "ShortUrl id[%s] was deleted from database", shortUrl.getId())))
                    .doOnError(e -> LOGGER.warning(EVENT_FAILURE, String.format(
                            "Error deleting ShortUrl id[%s] from database: [%s]", shortUrl.getId(), e.getMessage())))
                    .thenReturn(shortUrl)
                    .onErrorReturn(shortUrl);
        })
        .blockLast();
    }
}
