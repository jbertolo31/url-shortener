package com.jasonbertolo.urlshortener.web.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jasonbertolo.urlshortener.web.config.settings.MicroservicesSettings;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.owasp.esapi.Logger.*;

@Service
public class ApiService {

    /*
     * Models
     */
    public record ApiResponse<T>(String status, String message, T data){}

    public record ShortUrlResponse(String id, String key, String url, String description,
                                   @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
                                   @JsonFormat(shape = JsonFormat.Shape.STRING) Instant lastUpdatedAt,
                                   @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt){}

    public static class ApiException extends RuntimeException {
        public final transient ProblemDetail problemDetail;

        public ApiException(ProblemDetail problemDetail) {
            super(problemDetail.getDetail());
            this.problemDetail = problemDetail;
        }
    }


    /*
     * Implementation
     */
    private static final Logger LOGGER = ESAPI.getLogger(ApiService.class.getSimpleName());

    public static final ParameterizedTypeReference<ApiResponse<ShortUrlResponse>> SHORT_URL_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient apiWebClient;
    private final MicroservicesSettings microservicesSettings;

    public ApiService(@Qualifier("apiWebClient") WebClient apiWebClient, MicroservicesSettings microservicesSettings) {
        this.apiWebClient = apiWebClient;
        this.microservicesSettings = microservicesSettings;
    }

    public Mono<ApiResponse<ShortUrlResponse>> getAndCacheShortUrl(String key) {
        LOGGER.debug(EVENT_UNSPECIFIED, String.format("Requesting API for ShortUrl key[%s]", key));
        return apiWebClient.get().uri(microservicesSettings.getApiBaseUrl() + "/api/v1/cache/{key}", key)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(SHORT_URL_RESPONSE_TYPE)
                                .doOnNext(r -> LOGGER.debug(EVENT_SUCCESS, String.format(
                                        "API request success: [%s]: %s", response.statusCode().value(), r)));
                    } else {
                        return response.bodyToMono(ProblemDetail.class)
                                .doOnNext(r -> LOGGER.warning(EVENT_FAILURE, String.format(
                                        "API request failed: [%s]: %s", response.statusCode().value(), r)))
                                .flatMap(problemDetail -> Mono.error(new ApiException(problemDetail)));
                    }
                });
    }
}
