package com.jasonbertolo.urlshortener.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jasonbertolo.urlshortener.api.config.settings.MicroservicesSettings;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.exception.*;
import com.jasonbertolo.urlshortener.api.model.dto.ApiResponse;
import com.jasonbertolo.urlshortener.api.util.ReactiveRequestContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiApplicationTest {

    // Some additional tests that aren't particularly useful aside from providing coverage to reduce noisy coverage
    // output. If possible coverage reports should aim for 95%+.

    @Autowired
    private MicroservicesSettings microservicesSettings;

    @Autowired
    private UrlShortenerSettings urlShortenerSettings;

    @BeforeEach
    void beforeEach() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
    }

    @Test
    void contextLoads() {
        assertThat(microservicesSettings).isNotNull();
        assertThat(urlShortenerSettings).isNotNull();
    }

    @Test
    @DisplayName("Utils private constructors")
    void utilsPrivateConstructors() throws NoSuchMethodException {
        Constructor<ReactiveRequestContextHolder> constructor = ReactiveRequestContextHolder.class
                .getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    @DisplayName("Model Constructors/Getters/Setters")
    void modelConstructorsGettersSettings() {
        assertThat(microservicesSettings.getApiBaseUrl()).isNotNull();
        assertThat(microservicesSettings.getAuthBaseUrl()).isNotNull();
        assertThat(microservicesSettings.getBffBaseUrl()).isNotNull();

        assertThat(urlShortenerSettings.getScheduledMaintenance().getCronZone()).isNotNull();
        assertThat(urlShortenerSettings.getScheduledMaintenance().getCleanupCron()).isNotNull();

        assertDoesNotThrow(() -> {
            ApiResponse<String> apiResponse = new ApiResponse<>("mesage", "data");
            apiResponse.setStatus("ok");
            apiResponse.setMessage("message");
            apiResponse.setData("data");
            assertThat(apiResponse).isNotNull();
        });
        assertDoesNotThrow(() -> new ApiResponse<>("ok", "some message", "data"));
    }

    @ParameterizedTest(name="exception={0}")
    @ValueSource(strings={
            "AccessDeniedException",
            "InvalidKeyException",
            "InvalidParameterException",
            "InvalidShortUrlException",
            "KeyExistsException",
            "ResourceNotFoundException"
    })
    @DisplayName("Exception Constructors/Getters/Setters")
    void exceptionConstructorsGettersSettings(String exceptionName) {
        switch (exceptionName) {
            case "AccessDeniedException" -> assertDoesNotThrow(() -> {
                Throwable t = new AccessDeniedException("ok", URI.create("https://www"), new Exception());
                assertThat(t.getMessage()).isNotNull();

                Throwable t2 = new AccessDeniedException("message");
                assertThat(t2.getMessage()).isNotNull();

                Throwable t3 = new AccessDeniedException("message", URI.create("https://www"));
                assertThat(t3.getMessage()).isNotNull();
            });
            case "InvalidKeyException" -> assertDoesNotThrow(() -> {
                Throwable t = new InvalidKeyException("ok", URI.create("https://www"), new Exception());
                assertThat(t.getMessage()).isNotNull();

                Throwable t2 = new InvalidKeyException("message");
                assertThat(t2.getMessage()).isNotNull();
            });
            case "InvalidParameterException" -> assertDoesNotThrow(() -> {
                Throwable t = new InvalidParameterException("ok", URI.create("https://www"), new Exception());
                assertThat(t.getMessage()).isNotNull();

                Throwable t2 = new InvalidParameterException("message");
                assertThat(t2.getMessage()).isNotNull();
            });
            case "InvalidShortUrlException" -> assertDoesNotThrow(() -> {
                Throwable t = new InvalidShortUrlException("ok", URI.create("https://www"), new Exception());
                assertThat(t.getMessage()).isNotNull();

                Throwable t2 = new InvalidShortUrlException("message");
                assertThat(t2.getMessage()).isNotNull();
            });
            case "KeyExistsException" -> assertDoesNotThrow(() -> {
                Throwable t = new KeyExistsException("ok", URI.create("https://www"), new Exception());
                assertThat(t.getMessage()).isNotNull();

                Throwable t2 = new KeyExistsException("message");
                assertThat(t2.getMessage()).isNotNull();

                Throwable t3 = new KeyExistsException("message", URI.create("https://www"));
                assertThat(t3.getMessage()).isNotNull();
            });
            case "ResourceNotFoundException" -> assertDoesNotThrow(() -> {
                Throwable t = new ResourceNotFoundException("ok", URI.create("https://www"), new Exception());
                assertThat(t.getMessage()).isNotNull();

                Throwable t2 = new ResourceNotFoundException("data");
                assertThat(t2.getMessage()).isNotNull();
            });
        }
    }
}
