package com.jasonbertolo.urlshortener.api.service;

import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.exception.*;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import com.jasonbertolo.urlshortener.api.model.dto.ErrorsDto;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlCreateDto;
import com.jasonbertolo.urlshortener.api.model.validation.ShortUrlValidator;
import com.jasonbertolo.urlshortener.api.repository.ShortUrlRepository;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SimpleErrors;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import static com.jasonbertolo.urlshortener.api.model.ShortUrl.KEY_ALLOWED_CHARS;
import static com.jasonbertolo.urlshortener.api.util.ReactiveRequestContextHolder.getRequestPath;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.owasp.esapi.Logger.*;

@Service
public class ShortUrlService {

    private static final Logger LOGGER = ESAPI.getLogger(ShortUrlService.class.getSimpleName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ShortUrlValidator SHORT_URL_VALIDATOR = new ShortUrlValidator();
    private static final int DEFAULT_MAX_ID_LENGTH = 50;
    private static final int DEFAULT_MAX_KEY_LENGTH = 50;
    private static final int DEFAULT_MAX_PAGE_NUMBER_LENGTH = 10000;
    private static final int DEFAULT_MAX_PAGE_SIZE_LENGTH = 10000;

    private final ShortUrlRepository shortUrlRepository;
    private final ReactiveRedisOperations<String, ShortUrl> reactiveRedisOperations;
    private final UrlShortenerSettings urlShortenerSettings;
    private final Pattern validKeyPattern;

    @Autowired
    public ShortUrlService(ShortUrlRepository shortUrlRepository,
                           ReactiveRedisOperations<String, ShortUrl> reactiveRedisOperations,
                           UrlShortenerSettings urlShortenerSettings) {
        this.shortUrlRepository = shortUrlRepository;
        this.reactiveRedisOperations = reactiveRedisOperations;
        this.urlShortenerSettings = urlShortenerSettings;
        this.validKeyPattern = Pattern.compile("[a-zA-Z0-9]{" + urlShortenerSettings.getKeyLength() + "}");
    }

    public Mono<ShortUrl> createShortUrl(ShortUrlCreateDto dto, String username) {
        return validateShortUrlDto(dto)
                .doOnSuccess(s -> LOGGER.debug(EVENT_UNSPECIFIED, String.format(
                        "Creating ShortUrl url[%s] for user[%s]", dto.getUrl(), username)))
                .then(generateKey(urlShortenerSettings.getKeyLength()))
                // Check if generated key already exists, throw error
                .flatMap(randomKey -> shortUrlRepository.findByKeyAndExpiresAtAfter(randomKey, Instant.now())
                        .flatMap(shortUrl -> getRequestPath().flatMap(uri -> Mono.error(new KeyExistsException(
                                String.format("Duplicate key[%s] found", shortUrl.getKey()), uri))))
                        .switchIfEmpty(Mono.defer(() -> shortUrlRepository.save(new ShortUrl.Builder()
                                .url(dto.getUrl())
                                .key(randomKey)
                                .description(dto.getDescription())
                                .expiresAt(Instant.now().plus(urlShortenerSettings.getUrlTtlDays(), DAYS))
                                .build()))))
                .retry(2L)
                .cast(ShortUrl.class)
                .doOnSuccess(shortUrl -> LOGGER.info(EVENT_SUCCESS, String.format(
                        "Created ShortUrl id[%s] for user[%s]", shortUrl.getId(), username)))
                .doOnError(e -> LOGGER.error(EVENT_FAILURE, String.format(
                        "Failed to create ShortUrl url[%s] for user[%s]", dto.getUrl(), username)));
    }

    public Mono<ShortUrl> getUserShortUrl(String id, String username) {
        return validateId(id)
                .doOnSuccess(s -> LOGGER.debug(EVENT_UNSPECIFIED, String.format(
                        "Getting ShortUrl by id[%s] for user[%s]", id, username)))
                .then(shortUrlRepository.findByIdAndCreatedByAndExpiresAtAfter(id, username, Instant.now()))
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new ResourceNotFoundException(String.format(
                        "ShortUrl id[%s] and user[%s] was not found", id, username), uri))));
    }

    public Mono<PageImpl<ShortUrl>> getUserShortUrls(String page, String size, String username) {
        return validatePageParams(page, size)
                .doOnSuccess(s -> LOGGER.debug(EVENT_UNSPECIFIED, String.format(
                        "Listing ShortUrls page[%s], size[%s] for user[%s]", page, size, username)))
                .flatMap(pageRequest -> Mono.zip(
                        shortUrlRepository.findByCreatedByAndExpiresAtAfterOrderByCreatedAtDesc(
                                username, Instant.now(), pageRequest).collectList(),
                        Mono.just(pageRequest),
                        shortUrlRepository.countByCreatedByAndExpiresAtAfter(username, Instant.now())
                ))
                .map(t -> new PageImpl<>(t.getT1(), t.getT2(), t.getT3()));
    }

    public Mono<Void> deleteUserShortUrl(String id, String username) {
        return validateId(id)
                .doOnSuccess(s -> LOGGER.debug(EVENT_UNSPECIFIED, String.format(
                        "Deleting ShortUrl id[%s] for user[%s]", id, username)))
                .then(shortUrlRepository.findByIdAndCreatedByAndExpiresAtAfter(id, username, Instant.now()))
                .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new ResourceNotFoundException(
                        String.format("ShortUrl id[%s] and user[%s] was not found", id, username), uri))))
                .flatMap(shortUrl -> reactiveRedisOperations.opsForValue().delete(shortUrl.getKey()))
                .then(shortUrlRepository.deleteById(id))
                .doOnSuccess(v -> LOGGER.info(EVENT_SUCCESS, String.format(
                        "Deleted ShortUrl id[%s] for user[%s]", id, username)))
                .doOnError(e -> LOGGER.error(EVENT_FAILURE, String.format(
                        "Failed to delete ShortUrl id[%s] for user[%s]", id, username)));
    }

    public Mono<ShortUrl> getAndCacheShortUrl(String key) {
        return validateKey(key)
                .doOnSuccess(s -> LOGGER.debug(EVENT_UNSPECIFIED, String.format(
                        "Getting and caching ShortUrl by key[%s]", key)))
                // Check cache first
                .then(reactiveRedisOperations.opsForValue().get(key))
                // If empty, get from database and add to cache
                .switchIfEmpty(shortUrlRepository.findByKeyAndExpiresAtAfter(key, Instant.now())
                        .switchIfEmpty(getRequestPath().flatMap(uri -> Mono.error(new ResourceNotFoundException(
                                String.format("ShortUrl key[%s] was not found", key), uri)))))
                .flatMap(shortUrl -> reactiveRedisOperations.opsForValue().set(key, shortUrl,
                                Duration.ofDays(urlShortenerSettings.getCacheTtlDays()))

                        .thenReturn(shortUrl))
                .doOnSuccess(v -> LOGGER.info(EVENT_SUCCESS, String.format("Cached ShortUrl key[%s]", key)))
                .doOnError(e -> LOGGER.error(EVENT_FAILURE, String.format("Failed to cache ShortUrl key[%s]", key)));
    }

    private Mono<ShortUrlCreateDto> validateShortUrlDto(ShortUrlCreateDto dto) {
        BeanPropertyBindingResult beanPropertyBindingResult = new BeanPropertyBindingResult(
                dto, ShortUrlCreateDto.class.getSimpleName());
        SHORT_URL_VALIDATOR.validate(dto, beanPropertyBindingResult);
        if (beanPropertyBindingResult.getAllErrors().isEmpty()) {
            return Mono.just(dto);
        } else {
            SimpleErrors errors = new SimpleErrors(dto);
            errors.addAllErrors(beanPropertyBindingResult);
            return getRequestPath().flatMap(uri -> Mono.error(new InvalidShortUrlException(
                    "The request body to create a ShortUrl was invalid or contained errors.", uri)
                    .withErrors(new ErrorsDto(errors))));
        }
    }

    private Mono<String> generateKey(int length) {
        StringBuilder sb = new StringBuilder();
        char[] keyChars = KEY_ALLOWED_CHARS.toCharArray();
        for (int i=0; i<length; i++) {
            sb.append(keyChars[SECURE_RANDOM.nextInt(keyChars.length)]);
        }
        return Mono.just(sb.toString());
    }

    private Mono<String> validateId(String id) {
        if (id.matches("[A-Za-z0-9]+") && id.length() <= DEFAULT_MAX_ID_LENGTH) {
            return Mono.empty();
        }
        return getRequestPath().flatMap(uri -> Mono.error(new InvalidParameterException(String.format(
                "Id path variable[%s] is invalid, it should be alphanumberic.", id), uri)));
    }

    private Mono<String> validateKey(String key) {
        if (validKeyPattern.matcher(key).find() && key.length() <= DEFAULT_MAX_KEY_LENGTH) {
            return Mono.just(key);
        }
        return getRequestPath().flatMap(uri -> Mono.error(new InvalidKeyException(String.format(
                "ShortUrl key query parameter [%s] is invalid.", key), uri)));
    }

    private Mono<Pageable> validatePageParams(String pageParam, String sizeParam) {
        try {
            int page = Integer.parseInt(pageParam);
            int size = Integer.parseInt(sizeParam);
            if (page < 0 || page > DEFAULT_MAX_PAGE_NUMBER_LENGTH) {
                throw new IllegalArgumentException("Invalid page info");
            }
            if (size < 0 || size > DEFAULT_MAX_PAGE_SIZE_LENGTH) {
                throw new IllegalArgumentException("Invalid page info");
            }
            return Mono.just(PageRequest.of(page, size));
        } catch (IllegalArgumentException e) {
            return getRequestPath().flatMap(uri -> Mono.error(new InvalidParameterException(String.format(
                    "The paging info number[%s], size[%s] was invalid.", pageParam, sizeParam)
                    , uri)));
        }
    }
}
