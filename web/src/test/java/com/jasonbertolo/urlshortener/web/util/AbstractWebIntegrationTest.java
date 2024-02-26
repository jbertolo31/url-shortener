package com.jasonbertolo.urlshortener.web.util;

import com.jasonbertolo.urlshortener.web.config.settings.MicroservicesSettings;
import com.jasonbertolo.urlshortener.web.config.settings.UiSettings;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

public abstract class AbstractWebIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebIntegrationTest.class);

    @MockBean
    protected ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    protected UiSettings uiSettings;

    @Autowired
    protected MicroservicesSettings microservicesSettings;

    protected static MockWebServer apiMockWebServer;

    @DynamicPropertySource
    public static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("microservices.api-base-url", () -> apiMockWebServer.url("/").toString());
    }

    @BeforeAll
    static void beforeAllTests() throws IOException {
        LOGGER.info("Starting MockWebServer");
        apiMockWebServer = new MockWebServer();
        apiMockWebServer.start();
        LOGGER.info("MockWebServer running at {}", apiMockWebServer.url("/"));
    }

    @AfterAll
    static void afterAllTests() throws IOException {
        LOGGER.info("Stopping MockWebServer");
        apiMockWebServer.shutdown();
        LOGGER.info("MockWebServer was stopped");
    }
}
