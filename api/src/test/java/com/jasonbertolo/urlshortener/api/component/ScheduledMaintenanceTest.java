package com.jasonbertolo.urlshortener.api.component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import com.jasonbertolo.urlshortener.api.repository.ShortUrlRepository;
import com.jasonbertolo.urlshortener.api.service.ShortUrlService;
import com.jasonbertolo.urlshortener.api.util.AbstractApiIntegrationTest;
import com.jasonbertolo.urlshortener.api.util.ReactiveMongoRepositoryPopulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"url-shortener.scheduled-maintenance.cleanup-cron=-"})
class ScheduledMaintenanceTest extends AbstractApiIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ReactiveMongoRepositoryPopulator reactiveMongoRepositoryPopulator(
                ReactiveMongoOperations reactiveMongoOperations) {
            return new ReactiveMongoRepositoryPopulator(reactiveMongoOperations, ShortUrl.class,
                    "/data/sample-shorturls.json", ScheduledMaintenanceTest.class);
        }
    }

    @BeforeEach
    void beforeEach() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);

        // Cache all test ShortUrls
        reactiveMongoOperations.find(new Query(), ShortUrl.class)
                .flatMap(shortUrl -> reactiveRedisOperations.opsForValue().set(shortUrl.getKey(), shortUrl,
                        Duration.ofDays(urlShortenerSettings.getCacheTtlDays()))).blockLast();
    }

    @Autowired
    ShortUrlService shortUrlService;

    @Autowired
    ScheduledMaintenance scheduledMaintenance;

    @Mock
    ShortUrlRepository mockShortUrlRepository;

    @Mock
    ReactiveRedisOperations<String, ShortUrl> mockReactiveRedisOperations;

    @Test
    @DisplayName("Clean up")
    void cleanUp() {
        StepVerifier.create(reactiveMongoOperations.findById("object004", ShortUrl.class))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(reactiveRedisOperations.opsForValue().get("key004"))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(reactiveMongoOperations.findById("object011", ShortUrl.class))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(reactiveRedisOperations.opsForValue().get("key011"))
                .expectNextCount(1)
                .verifyComplete();

        assertDoesNotThrow(scheduledMaintenance::cleanup);

        StepVerifier.create(reactiveMongoOperations.findById("object004", ShortUrl.class))
                .expectNextCount(0)
                .verifyComplete();
        StepVerifier.create(reactiveRedisOperations.opsForValue().get("key004"))
                .expectNextCount(0)
                .verifyComplete();
        StepVerifier.create(reactiveMongoOperations.findById("object011", ShortUrl.class))
                .expectNextCount(0)
                .verifyComplete();
        StepVerifier.create(reactiveRedisOperations.opsForValue().get("key011"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Clean up, resume redis errors")
    void cleanUpResumeRedisError() {
        UrlShortenerSettings testUrlShortenerSettings = new UrlShortenerSettings();
        UrlShortenerSettings.ScheduledMaintenance mScheduledMaintenancesettings =
                new UrlShortenerSettings.ScheduledMaintenance();
        mScheduledMaintenancesettings.setCleanupEnabled(true);
        testUrlShortenerSettings.setScheduledMaintenance(mScheduledMaintenancesettings);

        when(mockShortUrlRepository.findByExpiresAtBefore(any())).thenReturn(Flux.fromIterable(mockExpiredShortUrls()));
        when(mockReactiveRedisOperations.delete(any(String[].class)))
                .thenReturn(Mono.error(new IllegalStateException("Mock redis exception")));
        when(mockShortUrlRepository.delete(any())).thenReturn(Mono.empty());

        ScheduledMaintenance mockScheduledMaintenance = new ScheduledMaintenance(mockShortUrlRepository,
                mockReactiveRedisOperations, testUrlShortenerSettings);

        assertDoesNotThrow(mockScheduledMaintenance::cleanup);

        verify(mockShortUrlRepository, times(1)).findByExpiresAtBefore(any());
        verify(mockReactiveRedisOperations, times(3)).delete(any(String[].class));
        verify(mockShortUrlRepository, times(3)).delete(any());
    }

    @Test
    @DisplayName("Clean up, resume mongo errors")
    void cleanUpResumeMongoError() {
        UrlShortenerSettings testUrlShortenerSettings = new UrlShortenerSettings();
        UrlShortenerSettings.ScheduledMaintenance mScheduledMaintenancesettings =
                new UrlShortenerSettings.ScheduledMaintenance();
        mScheduledMaintenancesettings.setCleanupEnabled(true);
        testUrlShortenerSettings.setScheduledMaintenance(mScheduledMaintenancesettings);

        when(mockShortUrlRepository.findByExpiresAtBefore(any())).thenReturn(Flux.fromIterable(mockExpiredShortUrls()));
        when(mockReactiveRedisOperations.delete(any(String[].class))).thenReturn(Mono.just(1L));
        when(mockShortUrlRepository.delete(any()))
                .thenReturn(Mono.error(new IllegalStateException("Mock mongo exception")));

        ScheduledMaintenance mockScheduledMaintenance = new ScheduledMaintenance(mockShortUrlRepository,
                mockReactiveRedisOperations, testUrlShortenerSettings);

        assertDoesNotThrow(mockScheduledMaintenance::cleanup);

        verify(mockShortUrlRepository, times(1)).findByExpiresAtBefore(any());
        verify(mockReactiveRedisOperations, times(3)).delete(any(String[].class));
        verify(mockShortUrlRepository, times(3)).delete(any());
    }

    @Test
    @DisplayName("Clean up, disabled")
    void cleanUpDisabled() {
        UrlShortenerSettings testUrlShortenerSettings = new UrlShortenerSettings();
        UrlShortenerSettings.ScheduledMaintenance mockScheduledMaintenancesettings =
                new UrlShortenerSettings.ScheduledMaintenance();
        mockScheduledMaintenancesettings.setCleanupEnabled(false);
        testUrlShortenerSettings.setScheduledMaintenance(mockScheduledMaintenancesettings);

        ScheduledMaintenance testScheduledMaintenance = new ScheduledMaintenance(mockShortUrlRepository,
                mockReactiveRedisOperations, testUrlShortenerSettings);

        assertDoesNotThrow(testScheduledMaintenance::cleanup);

        verify(mockShortUrlRepository, times(0)).findByExpiresAtBefore(any());
        verify(mockReactiveRedisOperations, times(0)).delete(any(String[].class));
        verify(mockShortUrlRepository, times(0)).delete(any());
    }

    private static List<ShortUrl> mockExpiredShortUrls() {
        ShortUrl shortUrl1 = new ShortUrl.Builder().url("https://example.com").key("key001")
                .expiresAt(Instant.now().minus(1, DAYS)).build();
        shortUrl1.setId("object001");

        ShortUrl shortUrl2 = new ShortUrl.Builder().url("https://example.com").key("key002")
                .expiresAt(Instant.now().minus(1, DAYS)).build();
        shortUrl1.setId("object002");

        ShortUrl shortUrl3 = new ShortUrl.Builder().url("https://example.com").key("key003")
                .expiresAt(Instant.now().minus(1, DAYS)).build();
        shortUrl1.setId("object003");

        return List.of(shortUrl1, shortUrl2, shortUrl3);
    }
}
