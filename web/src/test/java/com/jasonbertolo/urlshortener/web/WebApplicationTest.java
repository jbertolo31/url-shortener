package com.jasonbertolo.urlshortener.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jasonbertolo.urlshortener.web.config.settings.MicroservicesSettings;
import com.jasonbertolo.urlshortener.web.config.settings.UiSettings;
import com.jasonbertolo.urlshortener.web.handler.GlobalErrorHandler;
import com.jasonbertolo.urlshortener.web.service.ApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

@SpringBootTest
class WebApplicationTest {

    @TestConfiguration
    static class TestConfig {
        // Spring InMemoryReactiveClientRegistrationRepository fetches provider details from issuerUri on application
        // startup. Wiring up a mock ClientRegistration with the bean will prevent this behavior.
        @Bean
        public ReactiveClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("url_shortener")
                    .authorizationGrantType(AUTHORIZATION_CODE).clientId("clientId").redirectUri("redirectUri")
                    .authorizationUri("authorizationUri").tokenUri("tokenUri").build();
            return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
        }
    }

    private static final MockServerRequest MOCK_SERVER_REQUEST = MockServerRequest.builder().build();

    @Autowired
    private MicroservicesSettings microservicesSettings;

    @Autowired
    private UiSettings uiSettings;

    @BeforeEach
    void beforeEach() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
    }

    @Test
    void contextLoads() {
        assertThat(microservicesSettings).isNotNull();
        assertThat(uiSettings).isNotNull();
    }

    @Test
    @DisplayName("Model Constructors/Getters/Setters")
    void modelConstructorsGettersSettings() {
        assertThat(microservicesSettings.getApiBaseUrl()).isNotNull();
        assertThat(microservicesSettings.getAuthBaseUrl()).isNotNull();
        assertThat(microservicesSettings.getBffBaseUrl()).isNotNull();
    }

    @Test
    @DisplayName("GlobalErrorHandler ApiException")
    void globalErrorHandlerApiException() throws IOException {
        ErrorAttributes mockErrorAttributes = mock(ErrorAttributes.class);
        ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
        // Mock ClassLoader for Spring's application context
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        when(mockApplicationContext.getClassLoader()).thenReturn(mockClassLoader);
        when(mockClassLoader.getResources(anyString())).thenReturn(new Enumeration<>() {
            public boolean hasMoreElements() {return false;}
            public URL nextElement() {return null;}
        });

        ApiService.ApiException apiException = new ApiService.ApiException(
                ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, "Mock error"));
        when(mockErrorAttributes.getError(any())).thenReturn(apiException);
        GlobalErrorHandler globalErrorHandler = new GlobalErrorHandler(mockErrorAttributes, new WebProperties(),
                mockApplicationContext, mock(ServerCodecConfigurer.class),
                mock(ThymeleafReactiveViewResolver.class));

        Mono<ProblemDetail> testMono = globalErrorHandler.renderErrorResponse(MOCK_SERVER_REQUEST)
                .cast(EntityResponse.class)
                .map(EntityResponse::entity)
                .cast(ProblemDetail.class);

        StepVerifier.create(testMono)
                .consumeNextWith(problemDetail -> {
                    assertThat(problemDetail).isNotNull();
                    assertThat(problemDetail.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
                    assertThat(problemDetail.getDetail()).isEqualTo("Mock error");
                })
                .verifyComplete();
    }
}
