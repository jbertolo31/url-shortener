package com.jasonbertolo.urlshortener.api.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.net.UrlEscapers;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.exception.AccessDeniedException;
import com.jasonbertolo.urlshortener.api.exception.KeyExistsException;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlCreateDto;
import com.jasonbertolo.urlshortener.api.repository.ShortUrlRepository;
import com.jasonbertolo.urlshortener.api.service.ShortUrlService;
import com.jasonbertolo.urlshortener.api.util.AbstractApiIntegrationTest;
import com.jasonbertolo.urlshortener.api.util.ReactiveMongoRepositoryPopulator;
import com.jasonbertolo.urlshortener.api.util.WithMockApiUser;
import com.jasonbertolo.urlshortener.api.util.WithMockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import static com.jasonbertolo.urlshortener.api.model.dto.ApiResponse.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(properties = {"url-shortener.scheduled-maintenance.cleanup-enabled=false"})
@AutoConfigureWebTestClient
class ShortUrlHandlerTest extends AbstractApiIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ReactiveMongoRepositoryPopulator reactiveMongoRepositoryPopulator(
                ReactiveMongoOperations reactiveMongoOperations) {
            return new ReactiveMongoRepositoryPopulator(reactiveMongoOperations, ShortUrl.class,
                    "/data/sample-shorturls.json", ShortUrlHandlerTest.class);
        }
    }

    @Autowired
    WebTestClient webTestClient;

    String docsUrl;

    @BeforeEach
    void beforeEach() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);

        // Cache all test ShortUrls
        reactiveMongoOperations.find(new Query(), ShortUrl.class)
                .flatMap(shortUrl -> reactiveRedisOperations.opsForValue().set(shortUrl.getKey(), shortUrl,
                        Duration.ofDays(urlShortenerSettings.getCacheTtlDays()))).blockLast();

        docsUrl = urlShortenerSettings.getDocumentationBaseUrl();
    }

    /*
     * ******************************************************************************************
     * Create ShortUrl Tests
     * ******************************************************************************************
     */
    @Test
    @DisplayName("Create User ShortUrl - success")
    @WithMockApiUser
    void createUserShortUrlSuccess() {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://example.com?first=abc&second=def");
        dto.setDescription("Some description");

        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(OK)
                .jsonPath("$.message").isEqualTo("ShortUrlResponseDto")
                .jsonPath("$.data.id").exists()
                .jsonPath("$.data.key").exists()
                .jsonPath("$.data.url").isEqualTo(dto.getUrl())
                .jsonPath("$.data.description").isEqualTo(dto.getDescription())
                .jsonPath("$.data.created_at").exists()
                .jsonPath("$.data.last_updated_at").exists()
                .jsonPath("$.data.expires_at").exists()
                .jsonPath("$.data.created_by").doesNotExist();
    }

    @Test
    @DisplayName("Create User ShortUrl - no desc, success")
    @WithMockApiUser
    void createUserShortUrlNoDescSuccess() {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://example.com?first=abc&second=def");

        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(OK)
                .jsonPath("$.message").isEqualTo("ShortUrlResponseDto")
                .jsonPath("$.data.id").exists()
                .jsonPath("$.data.key").exists()
                .jsonPath("$.data.url").isEqualTo(dto.getUrl())
                .jsonPath("$.data.description").doesNotExist()
                .jsonPath("$.data.created_at").exists()
                .jsonPath("$.data.last_updated_at").exists()
                .jsonPath("$.data.expires_at").exists()
                .jsonPath("$.data.created_by").doesNotExist();
        }

    @Test
    @DisplayName("Create User ShortUrl - bad request, no body")
    @WithMockApiUser
    void createUserShortUrlBadRequestNoBody() {
        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidShortUrl")
                .jsonPath("$.title").isEqualTo("Invalid ShortUrl")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request body required")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @ParameterizedTest(name="url={0}")
    @ValueSource(strings={
            "This is not a url",
            "null",
            "100.200.300.1",
            "@somesite",
            "gs://bucket/name/dir/file.txt",
            "s3://bucket/name/dir/index.html",
            "htttp://localhost",
            "/Users/someone/index.html",
            "file:///Users/someone/index.html",
            "someone@example.com",
            "about:blank",
            "chrome://settings"
    })
    @DisplayName("Create User ShortUrl - bad request, invalid url")
    @WithMockApiUser
    void createUserShortUrlBadRequestUrl(String url) {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl(url);
        dto.setDescription("Some description");

        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidShortUrl")
                .jsonPath("$.title").isEqualTo("Invalid ShortUrl")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("The request body to create a ShortUrl was " +
                        "invalid or contained errors.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v))
                .jsonPath("$.errors").exists()
                .jsonPath("$.errors.field_errors").exists()
                .jsonPath("$.errors.field_errors[*].field").isEqualTo("url")
                .jsonPath("$.errors.field_errors[*].reference").isEqualTo(docsUrl +
                        "#ShortUrlCreateDto");
    }

    @Test
    @DisplayName("Create User ShortUrl - bad request, description too long")
    @WithMockApiUser
    void createUserShortUrlBadRequestDesc() {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://example.com?key=value");
        dto.setDescription("Some long description that is over 100 characters a long description long description " +
                "long description long description");

        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidShortUrl")
                .jsonPath("$.title").isEqualTo("Invalid ShortUrl")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("The request body to create a ShortUrl was " +
                        "invalid or contained errors.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v))
                .jsonPath("$.errors").exists()
                .jsonPath("$.errors.field_errors").exists()
                .jsonPath("$.errors.field_errors[0].field").isEqualTo("description")
                .jsonPath("$.errors.field_errors[0].rejected_value").isEqualTo(dto.getDescription())
                .jsonPath("$.errors.field_errors[0].reason").isEqualTo("description is too long")
                .jsonPath("$.errors.field_errors[0].reference").isEqualTo(docsUrl +
                        "#ShortUrlCreateDto");
    }

    @Test
    @DisplayName("Create User ShortUrl - unauthenticated")
    void createUserShortUrlUnauthenticated() {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://example.com?first=abc&second=def");
        dto.setDescription("Some description");

        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Create User ShortUrl - unauthorized, incorrect scopes")
    @WithMockApiUser(jwtScopes = {"cache:read", "cache:write"})
    void createUserShortUrlIncorrectScopes() {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://example.com?first=abc&second=def");
        dto.setDescription("Some description");

        webTestClient
                .post().uri("/api/v1/shorturl")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Create User ShortUrl - unauthenticated, no principal")
    void createUserShortUrlNoPrincipal() {
        ShortUrlService mockShortUrlService = mock(ShortUrlService.class);
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.principal()).thenReturn(Mono.empty());
        when(serverRequest.bodyToMono(ShortUrlCreateDto.class)).thenReturn(Mono.just(new ShortUrlCreateDto()));

        ShortUrlHandler shortUrlHandler = new ShortUrlHandler(mockShortUrlService, mock(UrlShortenerSettings.class));

        StepVerifier.create(shortUrlHandler.createUserShortUrl(serverRequest))
                .expectErrorMatches(t -> t instanceof AccessDeniedException)
                .verify();
    }

    @Test
    @DisplayName("Create User ShortUrl - conflict, duplicate key")
    @WithMockApiUser
    void createUserShortUrlDuplicateKey() {
        ShortUrlRepository mockSortUrlRepository = mock(ShortUrlRepository.class);
        ShortUrlService shortUrlService = new ShortUrlService(mockSortUrlRepository, reactiveRedisOperations,
                urlShortenerSettings);

        when(mockSortUrlRepository.findByKeyAndExpiresAtAfter(anyString(), any())).thenReturn(Mono.just(new ShortUrl()));

        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://example.com?first=abc&second=def");

        StepVerifier.create(shortUrlService.createShortUrl(dto, "user"))
                .expectErrorMatches(t -> t instanceof KeyExistsException)
                .verify();
    }



    /*
     * ******************************************************************************************
     * Get User ShortUrl Tests
     * ******************************************************************************************
     */
    @Test
    @DisplayName("Get User ShortUrl - success")
    @WithMockApiUser
    void getUserShortUrlSuccess() {
        webTestClient
                .get().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.message").isEqualTo("ShortUrlResponseDto")
                .jsonPath("$.data.id").isEqualTo("object001")
                .jsonPath("$.data.key").isEqualTo("key001")
                .jsonPath("$.data.url").value(startsWith("https://www.amazon.com/Mission-Impossible"))
                .jsonPath("$.data.description").isEqualTo("Short URL 1 for user")
                .jsonPath("$.data.created_at").isEqualTo("2024-02-10T10:05:05.500Z")
                .jsonPath("$.data.last_updated_at").exists()
                .jsonPath("$.data.expires_at").isEqualTo("2029-02-10T10:05:05.500Z")
                .jsonPath("$.data.created_by").doesNotExist();
    }

    @Test
    @DisplayName("Get User ShortUrl - bad request, id too long")
    @WithMockApiUser
    void getUserShortUrlBadRequestIdLength() {
        webTestClient
                .get().uri("/api/v1/shorturl/thisisareallylongidthatislongerthanfiftycharacterslong")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidParameter")
                .jsonPath("$.title").isEqualTo("Invalid parameter")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Id path variable[thisisareallylongidthatis" +
                        "longerthanfiftycharacterslong] is invalid, it should be alphanumberic.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/thisisareallylongidthatis" +
                        "longerthanfiftycharacterslong")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get User ShortUrl - bad request, id not alphanumeric")
    @WithMockApiUser
    void getUserShortUrlBadRequestIdNotAlphaNum() {
        webTestClient
                .get().uri("/api/v1/shorturl/@12345")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidParameter")
                .jsonPath("$.title").isEqualTo("Invalid parameter")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Id path variable[@12345] is invalid, it " +
                        "should be alphanumberic.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/@12345")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get User ShortUrl - resource not found")
    @WithMockApiUser
    void getUserShortUrlNotFound() {
        webTestClient
                .get().uri("/api/v1/shorturl/someObject")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=ResourceNotFound")
                .jsonPath("$.title").isEqualTo("Resource not found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("ShortUrl id[someObject] and user[user] was " +
                        "not found")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/someObject")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get User ShortUrl - resource not found, other user")
    @WithMockApiUser
    void getUserShortUrlNotFoundOtherUser() {
        webTestClient
                .get().uri("/api/v1/shorturl/object010") // Belongs to user2
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=ResourceNotFound")
                .jsonPath("$.title").isEqualTo("Resource not found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("ShortUrl id[object010] and user[user] was " +
                        "not found")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/object010")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get User ShortUrl - unauthenticated")
    void getUserShortUrlUnauthenticated() {
        webTestClient
                .get().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Get User ShortUrl - unauthorized, incorrect scopes")
    @WithMockApiUser(jwtScopes = {"cache:read", "cache:write"})
    void getUserShortUrlIncorrectScopes() {
        webTestClient
                .get().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Get User ShortUrl - unauthenticated, no principal")
    void getUserShortUrlNoPrincipal() {
        ShortUrlService mockShortUrlService = mock(ShortUrlService.class);
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.principal()).thenReturn(Mono.empty());

        ShortUrlHandler shortUrlHandler = new ShortUrlHandler(mockShortUrlService, mock(UrlShortenerSettings.class));

        StepVerifier.create(shortUrlHandler.getUserShortUrl(serverRequest))
                .expectErrorMatches(t -> t instanceof AccessDeniedException)
                .verify();
    }



    /*
     * ******************************************************************************************
     * List User ShortUrls Tests
     * ******************************************************************************************
     */
    @Test
    @DisplayName("Get User ShortUrls - success")
    @WithMockApiUser
    void getUserShortUrlsSuccess() {
        webTestClient
                .get().uri("/api/v1/shorturl")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.message").isEqualTo("PageImpl")
                .jsonPath("$.data.total_pages").isEqualTo(1)
                .jsonPath("$.data.number_of_elements").isEqualTo(3)
                .jsonPath("$.data.total_elements").isEqualTo(3)
                .jsonPath("$.data.number").isEqualTo(0)
                .jsonPath("$.data.size").isEqualTo(20)
                .jsonPath("$.data.content[*].id").value(hasItems("object003", "object002", "object001"))
                .jsonPath("$.data.content[*].key").value(hasItems("key003", "key002", "key001"))
                .jsonPath("$.data.content[*].url").value(hasItems(startsWith("https://")))
                .jsonPath("$.data.content[*].description").value(hasItems(startsWith("Short URL")))
                .jsonPath("$.data.content[*].created_by").doesNotExist();
    }

    @Test
    @DisplayName("Get User ShortUrls Pagination - success")
    @WithMockApiUser
    void getUserShortUrlsPaginationSuccess() {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/shorturl")
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.message").isEqualTo("PageImpl")
                .jsonPath("$.data.total_pages").isEqualTo(2)
                .jsonPath("$.data.total_elements").isEqualTo(3)
                .jsonPath("$.data.number_of_elements").isEqualTo(2)
                .jsonPath("$.data.number").isEqualTo(0)
                .jsonPath("$.data.size").isEqualTo(2)
                .jsonPath("$.data.content[*].id").value(hasItems("object003", "object002"))
                .jsonPath("$.data.content[*].key").value(hasItems("key003", "key002"))
                .jsonPath("$.data.content[*].url").value(hasItems(startsWith("https://")))
                .jsonPath("$.data.content[*].description").value(hasItems(startsWith("Short URL")))
                .jsonPath("$.data.content[*].created_by").doesNotExist();
    }


    static Stream<Arguments> testPageParams() {
        return Stream.of(
                Arguments.of("@", "0"),
                Arguments.of("0", "@"),
                Arguments.of("1", "0"),
                Arguments.of("999999", "1"),
                Arguments.of("1", "999999"),
                Arguments.of("-1", "1"),
                Arguments.of("1", "-1")
        );
    }
    @ParameterizedTest(name="page={0}, size={1}")
    @MethodSource("testPageParams")
    @DisplayName("Get User ShortUrls - bad request, invalid page params")
    @WithMockApiUser
    void getUserShortUrlsBadRequestPageable(String page, String size) {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/shorturl")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidParameter")
                .jsonPath("$.title").isEqualTo("Invalid parameter")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo(String.format(
                        "The paging info number[%s], size[%s] was invalid.", page, size))
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get User ShortUrls - bad request, invalid page params")
    @WithMockApiUser
    void getUserShortUrlsBadRequestPageableSize() {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/shorturl")
                        .queryParam("page", "0")
                        .queryParam("size", "1000000")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidParameter")
                .jsonPath("$.title").isEqualTo("Invalid parameter")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("The paging info number[0], size[1000000] " +
                        "was invalid.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get User ShortUrls - unautheticated")
    void getUserShortUrlsUnauthenticated() {
        webTestClient
                .get().uri("/api/v1/shorturl")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Get User ShortUrls - unauthorized, incorrect scopes")
    @WithMockApiUser(jwtScopes = {"cache:read", "cache:write"})
    void getUserShortUrlsIncorrectScopes() {
        webTestClient
                .get().uri("/api/v1/shorturl")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Get User ShortUrls - unauthenticated, no principal")
    void getUserShortUrlsNoPrincipal() {
        ShortUrlService mockShortUrlService = mock(ShortUrlService.class);
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.principal()).thenReturn(Mono.empty());

        ShortUrlHandler shortUrlHandler = new ShortUrlHandler(mockShortUrlService, mock(UrlShortenerSettings.class));

        StepVerifier.create(shortUrlHandler.getUserShortUrls(serverRequest))
                .expectErrorMatches(t -> t instanceof AccessDeniedException)
                .verify();
    }



    /*
     * ******************************************************************************************
     * Delete User ShortUrl Tests
     * ******************************************************************************************
     */
    @Test
    @DisplayName("Delete User ShortUrl - success")
    @WithMockApiUser
    void deleteUserShortUrlSuccess() {
        webTestClient
                .delete().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isNoContent();

        StepVerifier
                .create(reactiveRedisOperations.opsForValue().get("key001"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete User ShortUrl - bad request, id too long")
    @WithMockApiUser
    void deleteUserShortUrlBadRequestIdLength() {
        webTestClient
                .delete().uri("/api/v1/shorturl/thisisareallylongidthatislongerthanfiftycharacterslong")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidParameter")
                .jsonPath("$.title").isEqualTo("Invalid parameter")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Id path variable[thisisareallylongidthatis" +
                        "longerthanfiftycharacterslong] is invalid, it should be alphanumberic.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/thisisareallylongidthatis" +
                        "longerthanfiftycharacterslong")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Delete User ShortUrl - bad request, id not alphanumeric")
    @WithMockApiUser
    void deleteUserShortUrlBadRequestIdNotAlphaNum() {
        webTestClient
                .delete().uri("/api/v1/shorturl/$12345")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidParameter")
                .jsonPath("$.title").isEqualTo("Invalid parameter")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Id path variable[$12345] is invalid, it " +
                        "should be alphanumberic.")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/$12345")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Delete User ShortUrl - resource not found")
    @WithMockApiUser
    void deleteUserShortUrlNotFound() {
        webTestClient
                .delete().uri("/api/v1/shorturl/someObject")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=ResourceNotFound")
                .jsonPath("$.title").isEqualTo("Resource not found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("ShortUrl id[someObject] and user[user] was " +
                        "not found")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/someObject")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Delete User ShortUrl - resource not found, other user")
    @WithMockApiUser
    void deleteUserShortUrlNotFoundOtherUser() {
        webTestClient
                .delete().uri("/api/v1/shorturl/object010") // Belongs to user2
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=ResourceNotFound")
                .jsonPath("$.title").isEqualTo("Resource not found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("ShortUrl id[object010] and user[user] was " +
                        "not found")
                .jsonPath("$.instance").isEqualTo("/api/v1/shorturl/object010")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Delete User ShortUrl - unauthenticated")
    void deleteUserShortUrlUnauthenticated() {
        webTestClient
                .delete().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Delete User ShortUrl - unauthorized, incorrect scopes")
    @WithMockApiUser(jwtScopes = {"cache:read", "cache:write"})
    void deleteUserShortUrlIncorrectScopes() {
        webTestClient
                .delete().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Delete User ShortUrl - unauthenticated, no principal")
    void deleteUserShortUrlNoPrincipal() {
        ShortUrlService mockShortUrlService = mock(ShortUrlService.class);
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.principal()).thenReturn(Mono.empty());

        ShortUrlHandler shortUrlHandler = new ShortUrlHandler(mockShortUrlService, mock(UrlShortenerSettings.class));

        StepVerifier.create(shortUrlHandler.deleteUserShortUrl(serverRequest))
                .expectErrorMatches(t -> t instanceof AccessDeniedException)
                .verify();
    }



    /*
     * ******************************************************************************************
     * Cache ShortUrl Tests
     * ******************************************************************************************
     */
    @Test
    @DisplayName("Get and cache ShortUrl - success")
    @WithMockClient
    void getAndCacheShortUrlSuccess() {
        webTestClient
                .get().uri("/api/v1/cache/key001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.message").isEqualTo("ShortUrlResponseDto")
                .jsonPath("$.data.id").isEqualTo("object001")
                .jsonPath("$.data.key").isEqualTo("key001")
                .jsonPath("$.data.url").value(startsWith("https://www.amazon.com/Mission-Impossible"))
                .jsonPath("$.data.description").isEqualTo("Short URL 1 for user")
                .jsonPath("$.data.created_at").isEqualTo("2024-02-10T10:05:05.500Z")
                .jsonPath("$.data.last_updated_at").exists()
                .jsonPath("$.data.expires_at").isEqualTo("2029-02-10T10:05:05.500Z")
                .jsonPath("$.data.created_by").doesNotExist();

        String expected = "https://www.amazon.com/Mission-Impossible-Dead-Reckoning-Part/dp/B0B8RQDDHZ/ref=sr_1_4?" +
                "crid=2PPKS4IR8D4QN&dib=eyJ2IjoiMSJ9.Klwc3XdGqQPRhFiMHL5EfqI_lJPUu6LSTSkKS_LITZ3sygMzJ5bwzoDlM3BO5" +
                "VqcBuv3J1I7cxENg_yYZn3u0KKpPvdwbyEcc-DIfHFnGZ6aMHxeMqZopo3AslAt2nVM-bzP8AC4et4OvfaQt3YZSwKWc5cQZI" +
                "KxlTioqXlfoDjAyMTk9vdLGk490Zf9mDHImzfOipYr9mU-rtQRkEEZgK5ICVUj4R_DkjVs4qGk3jM.hXJsAVGVSUvT3qKieQ7" +
                "h5nhs2jkx_rOoXJGITb9NTi8&dib_tag=se&keywords=movies&qid=1708132384&sprefix=movies%2Caps%2C83&sr=8-4";
        StepVerifier
                .create(reactiveRedisOperations.opsForValue().get("key001"))
                .consumeNextWith(shortUrl -> {
                    assertThat(shortUrl).isNotNull();
                    assertThat(shortUrl.getUrl()).isEqualTo(expected);
                })
                .verifyComplete();
    }

    @ParameterizedTest(name="key={0}")
    @ValueSource(strings={
            "This is not a key",
            "+123+",
            "_key",
            "(key)",
            "@@@@@@@@@@@@@",
    })
    @DisplayName("Get and cache ShortUrl - bad request, invalid key")
    @WithMockClient
    void getAndCacheShortUrlBadRequestInvalidKey(String key) {
        String encodedKey = UrlEscapers.urlFragmentEscaper().escape(key);
        webTestClient
                .get().uri("/api/v1/cache/" + key)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidShortUrlKey")
                .jsonPath("$.title").isEqualTo("Invalid ShortUrl key")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("ShortUrl key query parameter ["+key+"] is " +
                        "invalid.")
                .jsonPath("$.instance").isEqualTo("/api/v1/cache/" + encodedKey)
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get and cache ShortUrl - bad request, key too long")
    @WithMockClient
    void getAndCacheShortUrlBadRequestKeyLength() {
        webTestClient
                .get().uri("/api/v1/cache/thisisareallylongidthatislongerthanfiftycharacterslong")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=InvalidShortUrlKey")
                .jsonPath("$.title").isEqualTo("Invalid ShortUrl key")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("ShortUrl key query parameter [thisisa" +
                        "reallylongidthatislongerthanfiftycharacterslong] is invalid.")
                .jsonPath("$.instance").isEqualTo("/api/v1/cache/thisisareallylongidthatis" +
                        "longerthanfiftycharacterslong")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get and cache ShortUrl - resource not found")
    @WithMockClient
    void getAndCacheShortUrlNotFound() {
        webTestClient
                .get().uri("/api/v1/cache/someObject")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.type").isEqualTo(docsUrl + "?problem=ResourceNotFound")
                .jsonPath("$.title").isEqualTo("Resource not found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("ShortUrl key[someObject] was not found")
                .jsonPath("$.instance").isEqualTo("/api/v1/cache/someObject")
                .jsonPath("$.timestamp").isNumber()
                .jsonPath("$.traceId").value(v -> assertEquals(UUID.fromString((String)v).toString(), v));
    }

    @Test
    @DisplayName("Get and cache ShortUrl - unauthenticated")
    void getAndCacheShortUrlUnauthenticated() {
        webTestClient
                .get().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Get and cache ShortUrl - unauthorized, incorrect scopes")
    @WithMockClient(jwtScopes = {"shorturl:read", "shorturl:write"})
    void getAndCacheShortUrlIncorrectScopes() {
        webTestClient
                .get().uri("/api/v1/cache/object001")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Get and cache ShortUrl - unauthorized, ApiUser")
    @WithMockApiUser(jwtScopes = {"cache:read", "cache:write"})
    void getAndCacheShortUrlUnauthorizedApiUser() {
        webTestClient
                .get().uri("/api/v1/shorturl/object001")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Get and cache ShortUrl - unauthenticated, no principal")
    void getAndCacheShortUrlNoPrincipal() {
        ShortUrlService mockShortUrlService = mock(ShortUrlService.class);
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.principal()).thenReturn(Mono.empty());

        ShortUrlHandler shortUrlHandler = new ShortUrlHandler(mockShortUrlService, mock(UrlShortenerSettings.class));

        StepVerifier.create(shortUrlHandler.getAndCacheShortUrl(serverRequest))
                .expectErrorMatches(t -> t instanceof AccessDeniedException)
                .verify();
    }
}
