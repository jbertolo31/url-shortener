package com.jasonbertolo.urlshortener.web.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonbertolo.urlshortener.web.config.settings.UiSettings;
import com.jasonbertolo.urlshortener.web.util.AbstractWebIntegrationTest;
import com.jasonbertolo.urlshortener.web.util.WithMockWebUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

@SpringBootTest
@AutoConfigureWebTestClient
class SupportHandlerTest extends AbstractWebIntegrationTest {

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

    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
    }

    @Test
    @DisplayName("Get Jwt - success")
    @WithMockWebUser(jwtScopes = {"openid", "profile", "shorturl:read", "shorturl:write"})
    void getJwt() {
        OAuth2AccessToken oAuth2AccessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "ey...", Instant.now(), Instant.now().plusSeconds(3600),
                Set.of("openid", "profile", "shorturl:read", "shorturl:write"));
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(mock(ClientRegistration.class),
                "user", oAuth2AccessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(Mono.just(authorizedClient));
        webTestClient
                .get().uri("/jwt")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token_value").value(startsWith("ey"))
                .jsonPath("$.issued_at").isNumber()
                .jsonPath("$.expires_at").isNumber()
                .jsonPath("$.token_type.value").isEqualTo("Bearer")
                .jsonPath("$.scopes").value(containsInAnyOrder("openid", "profile", "shorturl:write",
                        "shorturl:read"));
    }

    @Test
    @DisplayName("Get Jwt - unauthenticated")
    void getJwtUnauthenticated() {
        webTestClient
                .get().uri("/jwt")
                .exchange()
                .expectHeader().value(HttpHeaders.LOCATION, equalTo("/oauth2/authorization/url_shortener"))
                .expectStatus().is3xxRedirection();
    }

    @Test
    @DisplayName("Get Jwt - unauthorized, incorrect scopes")
    @WithMockWebUser(jwtScopes = {"cache:read", "cache:write"})
    void getJwtIncorrectScopes() {
        webTestClient
                .get().uri("/jwt")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Get Jwt - unauthorized, incorrect no profile scope")
    @WithMockWebUser(jwtScopes = {"openid"})
    void getJwtIncorrectScopes2() {
        webTestClient
                .get().uri("/jwt")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Get Config - success")
    @WithMockWebUser
    void getConfig() {
        byte[] responseBody = Optional.ofNullable(webTestClient
                .get().uri("/config.js")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody()).orElseThrow();
        String response = new String(responseBody);

        assertThat(response).contains("const CONFIG=");
    }

    @Test
    @DisplayName("Get Config - unauthenticated, success")
    void getConfigUnauthenticated() {
        byte[] responseBody = Optional.ofNullable(webTestClient
                .get().uri("/config.js")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody()).orElseThrow();
        String response = new String(responseBody);

        assertThat(response).contains("const CONFIG=");
    }

    @Test
    @DisplayName("Get Config - Config JSON exception")
    void getConfigJsonException() {
        UiSettings badUiSettings = new UiSettings();
        badUiSettings.setFrontendConfig(Map.of("key", new Object()));

        SupportHandler supportHandler = new SupportHandler(mock(ReactiveOAuth2AuthorizedClientManager.class),
                badUiSettings, new ObjectMapper());

        Mono<String> testMono = supportHandler.getUiConfig(mock(ServerRequest.class))
                .cast(EntityResponse.class)
                .map(EntityResponse::entity)
                .cast(String.class);

        StepVerifier.create(testMono)
                .consumeNextWith(config -> assertThat(config).isEqualTo("const CONFIG={}"))
                .verifyComplete();
    }
}
