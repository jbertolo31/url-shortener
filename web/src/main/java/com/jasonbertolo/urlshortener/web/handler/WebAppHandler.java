package com.jasonbertolo.urlshortener.web.handler;

import com.jasonbertolo.urlshortener.web.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerErrorException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.permanentRedirect;

@Component
public class WebAppHandler {

    private static final int DEFAULT_MAX_KEY_LENGTH = 50;

    private final ApiService apiService;
    private final Pattern validKeyPattern;

    @Autowired
    public WebAppHandler(ApiService apiService, @Value("${url-shortener.key-length}") String keyLength) {
        this.apiService = apiService;
        this.validKeyPattern = Pattern.compile("[a-zA-Z0-9]{" + keyLength + "}");
    }

    @NonNull
    public Mono<ServerResponse> indexPage(@SuppressWarnings("unused") ServerRequest serverRequest) {
        return ok().contentType(TEXT_HTML).render("index");
    }

    @NonNull
    public Mono<ServerResponse> myShortUrlsPage(@SuppressWarnings("unused") ServerRequest serverRequest) {
        return ok().contentType(TEXT_HTML).render("my-short-urls");
    }

    @NonNull
    public Mono<ServerResponse> redirect(ServerRequest serverRequest) {
        return cacheShortUrl(serverRequest)
                .flatMap(t -> permanentRedirect(URI.create(t.getT1())).build());
    }

    @NonNull
    public Mono<ServerResponse> revealPage(ServerRequest serverRequest) {
        return cacheShortUrl(serverRequest)
                .flatMap(t -> ok().contentType(TEXT_HTML).render("reveal",
                        Map.of("url", t.getT1(), "key", t.getT2())));
    }

    public Mono<Tuple2<String, String>> cacheShortUrl(ServerRequest serverRequest) {
        String key = serverRequest.pathVariable("key");
        return validateKey(key)
                .flatMap(apiService::getAndCacheShortUrl)
                .map(ApiService.ApiResponse::data)
                .map(ApiService.ShortUrlResponse::url)
                .onErrorMap(e -> {
                    if (e instanceof ApiService.ApiException apiException) {
                        HttpStatus status = Optional.ofNullable(HttpStatus.resolve(apiException.problemDetail.getStatus()))
                                .orElse(INTERNAL_SERVER_ERROR);
                        return switch (status) {
                            case BAD_REQUEST, NOT_FOUND -> new NoResourceFoundException(serverRequest.path());
                            default -> new ServerErrorException(serverRequest.path(), e);
                        };
                    } else {
                        return e;
                    }
                })
                .zipWith(Mono.just(key));
    }

    @NonNull
    public Mono<ServerResponse> errorPage(@SuppressWarnings("unused") ServerRequest serverRequest) {
        return ok().contentType(TEXT_HTML).render("error");
    }

    private Mono<String> validateKey(String key) {
        if (validKeyPattern.matcher(key).find() && key.length() <= DEFAULT_MAX_KEY_LENGTH) {
            return Mono.just(key);
        }
        return Mono.error(new NoResourceFoundException(String.format(
                "ShortUrl key query parameter [%s] is invalid.", key)));
    }
}
