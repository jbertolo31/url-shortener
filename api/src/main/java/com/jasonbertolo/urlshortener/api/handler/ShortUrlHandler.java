package com.jasonbertolo.urlshortener.api.handler;

import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.exception.AccessDeniedException;
import com.jasonbertolo.urlshortener.api.exception.InvalidShortUrlException;
import com.jasonbertolo.urlshortener.api.model.dto.ApiResponse;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlCreateDto;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlResponseDto;
import com.jasonbertolo.urlshortener.api.service.ShortUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Principal;

import static com.jasonbertolo.urlshortener.api.exception.AccessDeniedException.USER_AUTHENTICATION_REQUIRED;
import static com.jasonbertolo.urlshortener.api.util.ReactiveRequestContextHolder.getRequestPath;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class ShortUrlHandler {

    private final ShortUrlService shortUrlService;
    private final UrlShortenerSettings urlShortenerSettings;

    @Autowired
    public ShortUrlHandler(ShortUrlService shortUrlService, UrlShortenerSettings urlShortenerSettings) {
        this.shortUrlService = shortUrlService;
        this.urlShortenerSettings = urlShortenerSettings;
    }

    @NonNull
    public Mono<ServerResponse> createUserShortUrl(ServerRequest serverRequest) {
        return serverRequest.principal()
                .mapNotNull(Principal::getName)
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new AccessDeniedException(
                        USER_AUTHENTICATION_REQUIRED))))
                .zipWith(serverRequest.bodyToMono(ShortUrlCreateDto.class)
                        .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new InvalidShortUrlException(
                                "Request body required", uri)))))
                .flatMap(t -> shortUrlService.createShortUrl(t.getT2(), t.getT1()))
                .map(ShortUrlResponseDto::new)
                .flatMap(shortUrlResponseDto -> created(URI.create(urlShortenerSettings.getExternalAppUrl() +
                        "/" + shortUrlResponseDto.getKey())).contentType(APPLICATION_JSON)
                        .bodyValue(new ApiResponse<>(shortUrlResponseDto)));
    }

    @NonNull
    public Mono<ServerResponse> getUserShortUrl(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        return serverRequest.principal()
                .mapNotNull(Principal::getName)
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new AccessDeniedException(
                        USER_AUTHENTICATION_REQUIRED))))
                .flatMap(username -> shortUrlService.getUserShortUrl(id, username))
                .map(ShortUrlResponseDto::new)
                .flatMap(shortUrlResponseDto -> ok().contentType(APPLICATION_JSON)
                        .bodyValue(new ApiResponse<>(shortUrlResponseDto)));
    }

    @NonNull
    public Mono<ServerResponse> getUserShortUrls(ServerRequest serverRequest) {
        String page = serverRequest.queryParam("page").orElse("0");
        String size = serverRequest.queryParam("size").orElse("20");
        return serverRequest.principal()
                .mapNotNull(Principal::getName)
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new AccessDeniedException(
                        USER_AUTHENTICATION_REQUIRED))))
                .flatMap(principalName -> shortUrlService.getUserShortUrls(page, size, principalName)
                        .map(pg -> pg.map(ShortUrlResponseDto::new)))
                .map(ApiResponse::new)
                .flatMap(shortUrls -> ok().contentType(APPLICATION_JSON).bodyValue(shortUrls));
    }

    @NonNull
    public Mono<ServerResponse> deleteUserShortUrl(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        return serverRequest.principal()
                .mapNotNull(Principal::getName)
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new AccessDeniedException(
                        USER_AUTHENTICATION_REQUIRED))))
                .flatMap(principalName -> shortUrlService.deleteUserShortUrl(id, principalName))
                .then(noContent().build());
    }

    @NonNull
    public Mono<ServerResponse> getAndCacheShortUrl(ServerRequest serverRequest) {
        String key = serverRequest.pathVariable("key");
        return serverRequest.principal()
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new AccessDeniedException(
                        "Client credentials required"))))
                .flatMap(p -> shortUrlService.getAndCacheShortUrl(key))
                .map(ShortUrlResponseDto::new)
                .map(ApiResponse::new)
                .flatMap(shortUrls -> ok().contentType(APPLICATION_JSON).bodyValue(shortUrls));
    }
}
