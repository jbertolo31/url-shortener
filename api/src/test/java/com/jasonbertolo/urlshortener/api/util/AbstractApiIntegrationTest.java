package com.jasonbertolo.urlshortener.api.util;

import com.jasonbertolo.urlshortener.api.config.settings.MicroservicesSettings;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractApiIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApiIntegrationTest.class);

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:6.0.11"));

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:6.2.7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Autowired(required = false)
    protected ReactiveMongoRepositoryPopulator repositoryPopulator;

    @Autowired
    protected ReactiveMongoOperations reactiveMongoOperations;

    @Autowired
    protected ReactiveRedisOperations<String, ShortUrl> reactiveRedisOperations;

    @Autowired
    protected UrlShortenerSettings urlShortenerSettings;

    @Autowired
    protected MicroservicesSettings microservicesSettings;

    @BeforeAll
    static void beforeAllTests() {
        LOGGER.info("Starting MongoDB Testcontainer");
        mongoDBContainer.start();
        LOGGER.info("MongoDB Testcontainer running at {}", mongoDBContainer.getReplicaSetUrl());

        LOGGER.info("Starting Redis Testcontainer");
        redisContainer.start();
        LOGGER.info("Redis Testcontainer running at {}", redisContainer.getHost() + ":"
                +  redisContainer.getFirstMappedPort());
    }

    @AfterAll
    static void afterAllTests() {
        LOGGER.info("Stopping MongoDB Testcontainer");
        mongoDBContainer.stop();
        LOGGER.info("MongoDB Testcontainer was stopped");

        LOGGER.info("Stopping Redis Testcontainer");
        redisContainer.stop();
        LOGGER.info("Redis Testcontainer was stopped");
    }

    @BeforeEach
    void beforeEachTest() {
        if (repositoryPopulator != null) {
            LOGGER.info("Populating data from {}", repositoryPopulator.getResourcePath());
            repositoryPopulator.populate().block();
        }
    }

    @AfterEach
    void afterEachTest() {
        if (repositoryPopulator != null) {
            LOGGER.info("Deleting data for class {}", repositoryPopulator.getTypeClass().getSimpleName());
            repositoryPopulator.flushDb().block();
        }
        LOGGER.info("Deleting all redis data");
        reactiveRedisOperations.keys("*").map(k -> reactiveRedisOperations.opsForValue().delete(k)).blockLast();
    }
}
