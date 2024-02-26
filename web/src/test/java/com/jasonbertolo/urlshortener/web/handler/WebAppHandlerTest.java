package com.jasonbertolo.urlshortener.web.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jasonbertolo.urlshortener.web.util.AbstractWebIntegrationTest;
import com.jasonbertolo.urlshortener.web.util.WithMockWebUser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

@SpringBootTest
@AutoConfigureWebTestClient
class WebAppHandlerTest extends AbstractWebIntegrationTest {

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
    @DisplayName("Index page - success")
    @WithMockWebUser
    void indexPage() {
        Document doc = htmlPage("/");
        Element element = doc.select("p").get(0);
        assertThat(element.text()).startsWith("Got a really long URL");
    }

    @Test
    @DisplayName("Index page - unauthenticated, success")
    void indexPageUnauthenticated() {
        Document doc = htmlPage("/");
        Element element = doc.select("p").get(0);
        assertThat(element.text()).startsWith("Got a really long URL");
    }

    @Test
    @DisplayName("My Short URLs page - success")
    @WithMockWebUser
    void myShortUrlsPage() {
        Document doc = htmlPage("/my-short-urls");
        Element element = doc.getElementById("my-short-urls");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("My Short URLs page - unauthenticated")
    void myShortUrlsPageUnauthenticated() {
        webTestClient
                .get().uri("/my-short-urls")
                .exchange()
                .expectHeader().value(HttpHeaders.LOCATION, equalTo("/oauth2/authorization/url_shortener"))
                .expectStatus().is3xxRedirection();
    }

    @Test
    @DisplayName("Redirect - success")
    @WithMockWebUser
    void redirect() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_SUCCESS));

        authorizeClientCredentials();

        webTestClient
                .get().uri("/u/key001")
                .exchange()
                .expectHeader().value(HttpHeaders.LOCATION, equalTo("https://example.com?param=true"))
                .expectStatus().isPermanentRedirect();

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key001");
    }

    @Test
    @DisplayName("Redirect - unauthenticated, success")
    void redirectUnauthenticated() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_SUCCESS));

        authorizeClientCredentials();

        webTestClient
                .get().uri("/u/key001")
                .exchange()
                .expectHeader().value(HttpHeaders.LOCATION, equalTo("https://example.com?param=true"))
                .expectStatus().isPermanentRedirect();

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key001");
    }

    @Test
    @DisplayName("Redirect - failed, invalid key")
    void redirectInvalidKey() { //NOSONAR
        Document doc = htmlPage("/u/@@@111");
        Element element = doc.getElementsByClass("err-msg").get(0);
        assertThat(element.text()).startsWith("This page cannot be found");
    }

    @Test
    @DisplayName("Redirect - failed, key too long")
    void redirectKeyTooLong() {
        Document doc = htmlPage("/u/sometextsometextsometextsometextsometextsometextsometext");
        Element element = doc.getElementsByClass("err-msg").get(0);
        assertThat(element.text()).startsWith("This page cannot be found");
    }

    @Test
    @DisplayName("Redirect - failed, API 404 error")
    void redirectApiError404() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_ERROR_404));

        authorizeClientCredentials();

        Document doc = htmlPage("/u/key999");
        Element pEl = doc.getElementsByClass("err-msg").get(0);
        assertThat(pEl.text()).startsWith("This page cannot be found");

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key999");
    }

    @Test
    @DisplayName("Redirect - failed, API 500 error")
    void redirectApiError500() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_ERROR_500));

        authorizeClientCredentials();

        Document doc = htmlPage("/u/key001");
        Element pEl = doc.getElementsByClass("err-msg").get(0);
        assertThat(pEl.text()).startsWith("There was an error processing your request");

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key001");
    }

    @Test
    @DisplayName("Reveal - success")
    @WithMockWebUser
    void reveal() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_SUCCESS));

        authorizeClientCredentials();

        Document doc = htmlPage("/u/key001/r");
        Element pEl = doc.select("h5").get(0);
        assertThat(pEl.text()).startsWith("If you trust this link");

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key001");
    }

    @Test
    @DisplayName("Reveal - unauthenticated, success")
    void revealUnauthenticated() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_SUCCESS));

        authorizeClientCredentials();

        Document doc = htmlPage("/u/key001/r");
        Element pEl = doc.select("h5").get(0);
        assertThat(pEl.text()).startsWith("If you trust this link");

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key001");
    }

    @Test
    @DisplayName("Reveal - failed, invalid key")
    void revealInvalidKey() {
        Document doc = htmlPage("/u/@@@111/r");
        Element element = doc.getElementsByClass("err-msg").get(0);
        assertThat(element.text()).startsWith("This page cannot be found");
    }

    @Test
    @DisplayName("Reveal - failed, key too long")
    void revealKeyTooLong() {
        Document doc = htmlPage("/u/sometextsometextsometextsometextsometextsometextsometext/r");
        Element element = doc.getElementsByClass("err-msg").get(0);
        assertThat(element.text()).startsWith("This page cannot be found");
    }

    @Test
    @DisplayName("Reveal - failed, API 404 error")
    void revealApiError404() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_ERROR_404));

        authorizeClientCredentials();

        Document doc = htmlPage("/u/key999/r");
        Element pEl = doc.getElementsByClass("err-msg").get(0);
        assertThat(pEl.text()).startsWith("This page cannot be found");

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key999");
    }

    @Test
    @DisplayName("Reveal - failed, API 500 error")
    void revealApiError500() throws InterruptedException {
        apiMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(CACHE_API_RESPONSE_ERROR_500));

        authorizeClientCredentials();

        Document doc = htmlPage("/u/key001/r");
        Element pEl = doc.getElementsByClass("err-msg").get(0);
        assertThat(pEl.text()).startsWith("There was an error processing your request");

        RecordedRequest recordedRequest = apiMockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/cache/key001");
    }

    @Test
    @DisplayName("Error page - success")
    @WithMockWebUser
    void errorPage() {
        Document doc = htmlPage("/error");
        Element element = doc.select("h1").get(0);
        assertThat(element.text()).startsWith("Error!");
    }

    @Test
    @DisplayName("Error page - unauthenticated, success")
    void errorPageUnauthenticated() {
        Document doc = htmlPage("/error");
        Element element = doc.select("h1").get(0);
        assertThat(element.text()).startsWith("Error!");
    }

    private void authorizeClientCredentials() {
        OAuth2AccessToken oAuth2AccessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "ey...", Instant.now(), Instant.now().plusSeconds(3600),
                Set.of("cache:read", "cache:write"));
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(mock(ClientRegistration.class),
                "url_shortener", oAuth2AccessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(Mono.just(authorizedClient));
    }

    private Document htmlPage(String path) {
        byte[] responseBody = Optional.ofNullable(webTestClient
                .get().uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody()).orElse("".getBytes());
        String html = new String(responseBody);
        return Jsoup.parse(html);
    }

    private static final String CACHE_API_RESPONSE_SUCCESS =
            """
            {
                "status": "ok",
                "data": {
                    "id": "object001",
                    "key": "key001",
                    "url": "https://example.com?param=true",
                    "description": "Some description",
                    "created_at": "2024-02-10T22:05:05.220Z",
                    "last_updated_at": "2024-02-10T22:05:05.220Z",
                    "expires_at": "2029-02-09T22:05:05.220Z"
                }
            }
            """;

    private static final String CACHE_API_RESPONSE_ERROR_404 =
            """
            {
                "type": "http://localhost:8090/api/v1/docs/swagger-ui/index.html?problem=ResourceNotFound",
                "title": "Resource not found",
                "status": 404,
                "detail": "ShortUrl key[Ya7rF3Z] was not found",
                "instance": "/api/v1/cache/Ya7rF3Z",
                "timestamp": 1708767978025,
                "traceId": "b7c48324-a22a-44d2-af3e-231d071ee8de"
            }
            """;

    private static final String CACHE_API_RESPONSE_ERROR_500 =
            """
            {
                "status": 500,
                "detail": "An error occured"
            }
            """;
}
